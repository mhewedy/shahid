import model.Episode;
import model.Movie;
import model.Recent;
import model.Series;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.logging.PrintStreamLog;
import spark.Response;
import spark.ResponseTransformer;
import spark.utils.IOUtils;
import util.Config;
import util.Util;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static spark.Spark.*;

public class Api {

    public static void main(String[] args) {
        port(8801);

        staticFileLocation("/public");

        DBI dbi = new DBI(Config.JDBC_URL, Config.JDBC_USERNAME, Config.JDBC_PASSWORD);
        dbi.setSQLLog(new PrintStreamLog());

        final String[] NON_JSON_PATH = {"/"};


        get("/", (request, response) -> {
            response.type("text/html");
            String html = "";
            html += "<div><a href='/download/shahid-explorer.apk'>explorer</a></div>";
            html += "<div><a href='/download/shahid-player.apk'>player</a></div>";
            return html;
        } );

        get("/movie/tags/:id", (request, response) -> Movie.getAllTags(dbi, Integer.parseInt(request.params("id"))), json());

        get("/movie/tags", (request, response) -> Movie.getAllTagsNoLAUrl(dbi), json());

        get("/movie/tag", (request, response) -> new SearchResult(null, transform(Movie.findAllNoLAUrlByTag(dbi, request.queryParams("tag")))), json());

        get("/series/tags", (request, response) -> Series.getAllTags(dbi), json());

        get("/series/tag", (request, response) -> new SearchResult(transform(Series.findAllByTag(dbi, request.queryParams("tag"))), null), json());

        get("/search", (request, response) -> {
            List<List<?>> seriesList = transform(Series.search(dbi, request.queryParams("term")));
            List<List<?>> movieList = transform(Movie.search(dbi, request.queryParams("term")));
            return new SearchResult(seriesList, movieList);
        }, json());

        get("/series/recent", (request, response) -> new SearchResult(transform(Recent.findAll(dbi)), null), json());

        get("/episode/series/:id", (request, response) -> {
            int id = Integer.parseInt(request.params("id"));

            Series series = Series.findById(dbi, id);
            if (series != null) {
                series.setEpisodes(Episode.findAllBySeriesId(dbi, id));
                Recent.save(dbi, id);
            }
            return series;
        }, json());

        get("/episode/watched/:id", (request, response) -> {
            Episode.setAsWatched(dbi, Integer.parseInt(request.params("id")));
            return null;
        }, json());

        // /download/shahid-explorer.apk
        // /download/shahid-player.apk
        get("/download/:file", (request, response) -> download(response, "apk/" + request.params("file"), request.params("file")));

        after(((request, response) -> {

            if (Arrays.stream(NON_JSON_PATH).noneMatch(path -> path.equals(request.pathInfo()))){
                response.type("application/json; charset=utf-8");
                response.header("Access-Control-Allow-Origin", "*");
            }

        }));
    }

    private static Object download(Response response, String fileName, String downloadFileName)
            throws IOException {

        InputStream resourceAsStream = Api.class.getClassLoader().getResourceAsStream(fileName);
        response.header("Content-Disposition", "attachment; filename=" + downloadFileName);
        response.type("application/force-download");

        IOUtils.copy(resourceAsStream, response.raw().getOutputStream());

        response.raw().getOutputStream().flush();
        response.raw().getOutputStream().close();

        return response.raw();
    }

    private static List<List<?>> transform(List<?> list) {
        List<List<?>> ret = new ArrayList<>();

        List subList = null;

        for (int i = 0; i < list.size(); i++) {
            if (i % 2 == 0) {
                subList = new ArrayList<>();
                ret.add(subList);
                subList.add(list.get(i));
            } else {
                subList.add(list.get(i));
                subList = null;
            }
        }

        return ret;
    }

    private static ResponseTransformer json() {
        return (model) -> Util.GSON.toJson(model);
    }

    private static class SearchResult {
        private List<List<?>> seriesList;
        private List<List<?>> movieList;

        SearchResult(List<List<?>> seriesList, List<List<?>> movieList) {
            this.seriesList = seriesList;
            this.movieList = movieList;
        }
    }

}
