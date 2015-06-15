package io.geobit.opreturn;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import io.geobit.opreturn.entity.CoinSecretGrouped;


/**
 * Created by Riccardo Casatta @RCasatta on 19/04/15.
 */
public class StatsToJSONDataTable extends HttpServlet {
    private final static Logger log  = Logger.getLogger(StatsToJSONDataTable.class.getName());
    private final static Map<String,String> map= new HashMap<>();


    @Override
    public void init() throws ServletException {
        super.init();
        initMap();

    }

    private void initMap() {
        map.put("BIT", "Bitproof");
        map.put("DOC", "Docproof");
        map.put("FAC", "Factom");
        map.put("SPK", "Coinspark");
        map.put(new String(fromHex("4f4101")) , "OpenAsset");
        map.put("ASC", "Ascribe pool");
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            List<CoinSecretGrouped> coinSecretGroupedList = OfyService.ofy().load().type(CoinSecretGrouped.class).list();
            JSONObject dataTable = new JSONObject();

            JSONObject total       = new JSONObject();
            JSONArray totalCols = new JSONArray();
            JSONArray totalRows = new JSONArray();
            JSONObject totalCol0 = new JSONObject();
            JSONObject totalCol1 = new JSONObject();
            totalCol0.put("id", "x");
            totalCol1.put("id", "A");
            totalCol0.put("label", "date");
            totalCol1.put("label", "total");
            totalCol0.put("type", "date");
            totalCol1.put("type", "number");
            totalCols.put(totalCol0);
            totalCols.put(totalCol1);
            total.put("cols",totalCols);

            final Map<String,Integer> counters = new HashMap<>();

            for (CoinSecretGrouped coinSecretGrouped : coinSecretGroupedList) {
                String data= coinSecretGrouped.getDay();
                String year  = data.substring(0, 4);
                String month = data.substring(4, 6);
                String day   = data.substring(6, 8);
                String jDate = String.format("Date(%s,%d,%s)", year, Integer.parseInt(month)-1, day);

                JSONObject totalC   = new JSONObject();
                JSONArray totalCArr = new JSONArray();
                JSONObject totalV0  = new JSONObject();
                JSONObject totalV1  = new JSONObject();
                totalV0.put("v", jDate);
                totalV1.put("v", coinSecretGrouped.getTxWithOpReturn());
                totalCArr.put(totalV0);
                totalCArr.put(totalV1);
                totalC.put("c", totalCArr);
                totalRows.put(totalC);

                final String hexJSON = coinSecretGrouped.getJson();
                if(hexJSON!=null) {
                    JSONObject jsonObject = new JSONObject(hexJSON);
                    Iterator<?> keys = jsonObject.keys();
                    while (keys.hasNext()) {
                        final String hex = (String) keys.next();
                        int count = counters.containsKey(hex) ? counters.get(hex) : 0;
                        counters.put(hex, count + jsonObject.getInt(hex));
                    }
                }
            }
            total.put("rows",totalRows);

            final Map<String,Integer> moreThanXCounters = new HashMap<>();
            final Map<String,Integer> moreThanYCounters = new HashMap<>();

            for (Map.Entry<String, Integer> entry : counters.entrySet()) {
                if(entry.getValue()>400)
                    moreThanXCounters.put(  entry.getKey() ,entry.getValue());
                if(entry.getValue()>50)
                    moreThanYCounters.put(  entry.getKey() ,entry.getValue());
            }
            log.info("counters.size()=" + counters.size() + " moreThanXCounters.size()=" + moreThanXCounters.size());


            JSONObject byProto       = new JSONObject();
            JSONArray byProtoCols  = new JSONArray();
            JSONArray byProtoRows  = new JSONArray();
            JSONObject byProtoCol0 = new JSONObject();
            byProtoCol0.put("id", "x");
            byProtoCol0.put("label", "date");
            byProtoCol0.put("type", "date");
            byProtoCols.put(byProtoCol0);
            for (Map.Entry<String, Integer> entry : moreThanXCounters.entrySet()) {
                JSONObject byProtoColX = new JSONObject();
                final String o = entry.getKey();
                byProtoColX.put("id", o);
                byProtoColX.put("label", hexKeyToDesc(o) );
                byProtoColX.put("type", "number");
                byProtoCols.put(byProtoColX);
            }
            /*
            JSONObject byProtoColLast = new JSONObject();
            byProtoColLast.put("id", "other");
            byProtoColLast.put("label",  "OTHER" );
            byProtoColLast.put("type", "number");
            byProtoCols.put(byProtoColLast);
            */
            byProto.put("cols",byProtoCols);
            for (CoinSecretGrouped coinSecretGrouped : coinSecretGroupedList) {
                String data = coinSecretGrouped.getDay();
                String year = data.substring(0, 4);
                String month = data.substring(4, 6);
                String day = data.substring(6, 8);
                String jDate = String.format("Date(%s,%d,%s)", year, Integer.parseInt(month) - 1, day);
                JSONObject currC   = new JSONObject();
                JSONArray currCArr = new JSONArray();
                currC.put("c", currCArr);
                JSONObject currV0  = new JSONObject();
                currV0.put("v", jDate);
                currCArr.put(currV0);

                final String hexJSON = coinSecretGrouped.getJson();
                if(hexJSON!=null) {
                    JSONObject jsonObject = new JSONObject(hexJSON);
                    for (Map.Entry<String, Integer> entry : moreThanXCounters.entrySet()) {
                        final String key = entry.getKey();
                        final int val = jsonObject.has(key) ? jsonObject.getInt(key) : 0;
                        JSONObject currV1  = new JSONObject();
                        currV1.put("v", val);
                        currCArr.put(currV1);
                    }
                }
                byProtoRows.put(currC);
            }

            byProto.put("rows", byProtoRows);

            JSONObject cumulative  = new JSONObject();
            JSONArray cumulativeCols  = new JSONArray();
            JSONArray cumulativeRows  = new JSONArray();
            JSONObject cumulativeCol0 = new JSONObject();
            JSONObject cumulativeCol1 = new JSONObject();
            cumulativeCol0.put("id", "x");
            cumulativeCol1.put("id", "A");
            cumulativeCol0.put("label", "names");
            cumulativeCol1.put("label", "values");
            cumulativeCol0.put("type", "string");
            cumulativeCol1.put("type", "number");
            cumulativeCols.put(cumulativeCol0);
            cumulativeCols.put(cumulativeCol1);
            cumulative.put("cols", cumulativeCols);
            for (Map.Entry<String, Integer> entry : moreThanYCounters.entrySet()) {
                JSONObject cumulativeC   = new JSONObject();
                JSONArray cumulativeCArr = new JSONArray();
                JSONObject cumulativeV0 = new JSONObject();
                JSONObject cumulativeV1 = new JSONObject();

                cumulativeV0.put("v", hexKeyToDesc(entry.getKey()) );
                cumulativeV1.put("v", entry.getValue() );
                cumulativeCArr.put(cumulativeV0);
                cumulativeCArr.put(cumulativeV1);
                cumulativeC.put("c", cumulativeCArr);
                cumulativeRows.put(cumulativeC);
            }
            cumulative.put("rows",cumulativeRows);



            dataTable.put("total",total);
            dataTable.put("cumulative",cumulative);
            dataTable.put("proto",byProto);
            dataTable.put("counters", new JSONObject(moreThanXCounters) );

            resp.setContentType("application/json");
            resp.getWriter().write(dataTable.toString());


        } catch (JSONException e) {
            log.warning(e + " " + e.getMessage());
        }


    }


    private String hexKeyToDesc(String key) {
        final String val = new String(fromHex(key));
        if(map.containsKey(val))
            return map.get(val);
        else
            return val;

    }




    public static byte[] fromHex(String s) {
        if (s != null) {
            try {
                StringBuilder sb = new StringBuilder(s.length());
                for (int i = 0; i < s.length(); i++) {
                    char ch = s.charAt(i);
                    if (!Character.isWhitespace(ch)) {
                        sb.append(ch);
                    }
                }
                s = sb.toString();
                int len = s.length();
                byte[] data = new byte[len / 2];
                for (int i = 0; i < len; i += 2) {
                    int hi = (Character.digit(s.charAt(i), 16) << 4);
                    int low = Character.digit(s.charAt(i + 1), 16);
                    if (hi >= 256 || low < 0 || low >= 16) {
                        return null;
                    }
                    data[i / 2] = (byte) (hi | low);
                }
                return data;
            } catch (Exception ignored) {
            }
        }
        return null;
    }
}