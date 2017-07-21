import model.Episode;
import model.Recent;
import model.Series;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.logging.PrintStreamLog;
import spark.ResponseTransformer;
import spark.utils.IOUtils;
import util.Config;
import util.Util;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static spark.Spark.*;

public class Api {

    public static void main(String[] args) {
        port(8801);

        DBI dbi = new DBI(Config.JDBC_URL, Config.JDBC_USERNAME, Config.JDBC_PASSWORD);
        dbi.setSQLLog(new PrintStreamLog());

        get("/tags", (request, response) -> Series.getAllTags(dbi), json());

        get("/series/search", (request, response) -> transform(Series.search(dbi, request.queryParams("term"))), json());

        get("/recent", (request, response) -> transform(Recent.findAll(dbi)), json());

        get("/tag", (request, response) -> transform(Series.findAllByTag(dbi, request.queryParams("tag"))), json());

        get("/episode/series/:id", (request, response) -> {
            int id = Integer.parseInt(request.params("id"));

            Series series = Series.findById(dbi, id);
            if (series != null){
                series.setEpisodes(Episode.findAllBySeriesId(dbi, id));
                Recent.save(dbi, id);
            }
            return series;
        }, json());

        get("/episode/watched/:id", (request, response) -> {
            Episode.setAsWatched(dbi, Integer.parseInt(request.params("id")));
            return null;
        }, json());

        get("/download", (request, response) -> {
            InputStream resourceAsStream = Api.class.getClassLoader().getResourceAsStream("shahid-mobile.apk");
            response.header("Content-Disposition", "attachment; filename=shahid-mobile.apk");
            response.type("application/force-download");

            IOUtils.copy(resourceAsStream, response.raw().getOutputStream());

            response.raw().getOutputStream().flush();
            response.raw().getOutputStream().close();

            return response.raw();
        });

        after(((request, response) -> {
            response.type("application/json");
            response.header("Access-Control-Allow-Origin", "*");
        }));

    }

    private static List<List<Series>> transform(List<Series> seriesList) {
        List<List<Series>> ret = new ArrayList<>();

        List<Series> subList = null;

        for (int i = 0; i < seriesList.size(); i++) {
            if (i % 2 == 0) {
                subList = new ArrayList<>();
                ret.add(subList);
                subList.add(seriesList.get(i));
            } else {
                subList.add(seriesList.get(i));
                subList = null;
            }
        }

        return ret;
    }

    private static ResponseTransformer json() {
        return (model) -> Util.GSON.toJson(model);
    }

}
