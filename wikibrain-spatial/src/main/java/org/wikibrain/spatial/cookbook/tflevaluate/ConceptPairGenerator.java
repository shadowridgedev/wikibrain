//Write a prototype Java class to generate a set of concept pairs to ask a user given their specified location.
//        It should have one method: getConceptPair, which returns pairs of concepts to ask the user.
//        The method should take the user's "home" location (or a list of their home locations?) as an argument.
//        All concepts should be reasonably well-known (perhaps use the # of inlinks as a measure of concept popularity)
//        It should return concepts that are balanced in:
//        Spatial distance (just use straight-line distance for now)
//        Scale (Landmark vs City vs State vs Nation)
//        Semantic relatedness
//        To be clear, we'll want to pick a "stratified random sample" that is, we'll want to randomly sample, but guarantee we have some of each type
//        This code almost certainly won't be used for the actual survey, but prototyping it will help us think through the sticky issues.
//


package org.wikibrain.spatial.cookbook.tflevaluate;


import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;
import org.geotools.referencing.GeodeticCalculator;
import org.wikibrain.conf.Configuration;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.conf.Configurator;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.cmd.EnvBuilder;
import org.wikibrain.core.dao.*;
import org.wikibrain.core.dao.live.LocalLinkLiveDao;
import org.wikibrain.core.jooq.tables.WikidataEntityLabels;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.model.LocalLink;
import org.wikibrain.core.model.LocalPage;
import org.wikibrain.core.model.UniversalPage;
import org.wikibrain.spatial.core.dao.SpatialDataDao;
import org.wikibrain.sr.MonolingualSRMetric;
import org.wikibrain.sr.SRResult;
import org.wikibrain.wikidata.LocalWikidataStatement;
import org.wikibrain.wikidata.WikidataDao;
import org.wikibrain.wikidata.WikidataEntity;
import org.wikibrain.wikidata.WikidataStatement;

import java.awt.geom.Point2D;
import java.io.*;
import java.util.*;

/**
 * Created by maixa001 on 6/13/14.
 */
public class ConceptPairGenerator {

    private static int WIKIDATA_CONCEPTS = 1;
    private static int THRESHOLD = 30;

    private Env env;
    private final SpatialDataDao sdDao;
    private final LocalPageDao lpDao;
    private final UniversalPageDao upDao;
    private final WikidataDao wdao;
    private final Map<Integer, Geometry> geometries;
    private final MonolingualSRMetric sr;
    private final Language simple;
//    private final LocalLinkLiveDao lDao;
    private List<Integer> significantGeometries;

    public ConceptPairGenerator(Env env) throws Exception {
        this.env = env;
        Configurator c = env.getConfigurator();
        sdDao = c.get(SpatialDataDao.class);
        upDao = c.get(UniversalPageDao.class);
        lpDao = c.get(LocalPageDao.class);
        wdao = c.get(WikidataDao.class);
        geometries = sdDao.getAllGeometriesInLayer("wikidata", "earth");
        simple = Language.getByLangCode("simple");
        sr = c.get(
                MonolingualSRMetric.class, "ensemble",
                "language", simple.getLangCode());

    }

    public int[] getMaximumScoreConceptPair(Geometry home) {//throws Exception {

        // random first
        double random = Math.random();
        int first = (int) Math.floor(significantGeometries.size()*random);
        int firstId = significantGeometries.get(first);
        double max = 0;
        int maxid1 = 0, maxid2 = 0;

        // loop through significant geos for second point
        for (int secondId: significantGeometries){

            if (secondId != firstId ) {

                GeodeticCalculator calc = new GeodeticCalculator();
                Point firstPoint = (Point) geometries.get(firstId);
                calc.setStartingGeographicPoint(firstPoint.getX(), firstPoint.getY());
                Point secondPoint = (Point) geometries.get(secondId);
                calc.setDestinationGeographicPoint(secondPoint.getX(), secondPoint.getY());
                double distance = calc.getOrthodromicDistance() / 1000; //in km
                double score = 0;

                try {
                    UniversalPage u1 = upDao.getById(firstId, WIKIDATA_CONCEPTS);
                    UniversalPage u2 = upDao.getById(secondId, WIKIDATA_CONCEPTS);

                    SRResult similarity = sr.similarity(u1.getBestEnglishTitle(lpDao,true).getCanonicalTitle(), u2.getBestEnglishTitle(lpDao,true).getCanonicalTitle(), false);
                    score = similarity.getScore();

                    double finalScore = score/distance;

                    if (finalScore>max){
                        max = finalScore;
                        maxid1 = firstId;
                        maxid2 = secondId;
                    }

                }catch(Exception e){
                    System.out.println("Couldn't get universal pages");
                    System.out.println(e.toString());
                }
            }
        }

        return new int[] {maxid1, maxid2};

    }

    /**
     * Find two concepts within a certain distance of "home"
     * @param home
     * @param threshold In kilometers
     * @return
     */
    public int[] getRandomConceptPairWithinDistance(Point home, double threshold) {//throws Exception {

        // random first


        List<Integer> set = new ArrayList<Integer>();
        // loop through significant geos for second point
        for (int current: significantGeometries){

                GeodeticCalculator calc = new GeodeticCalculator();
                Point currentPoint = (Point) geometries.get(current);
                calc.setStartingGeographicPoint(currentPoint.getX(), currentPoint.getY());
                calc.setDestinationGeographicPoint(home.getX(), home.getY());

                double distance = calc.getOrthodromicDistance() / 1000; //in km
                double score = 0;

                if (distance<threshold){
                    set.add(current);
                }
        }
        if (set.size()<=1){
            return null;
        } else {
            int id1 = set.get((int) (Math.random() * set.size()));
            int id2 = id1;
            while (id2 == id1) {
                id2 = set.get((int) (Math.random() * set.size()));
            }
            System.out.println(set.size());

            return new int[]{id1, id2};
        }
    }


    public void extractSignificantGeometries(int threshold) throws  Exception{
        LocalLinkLiveDao linkDao = new LocalLinkLiveDao();
        //separate significant pages
        Set<Integer> keySet = geometries.keySet();
        significantGeometries = new ArrayList<Integer>();

        int count = 0;

        for (int geo : keySet) {
            count ++;
            if (count %1000 == 0){
                System.out.println("================================ "+count + "  ===================================");
            }
            // count inlinks
            try {
                UniversalPage uPage = upDao.getById(geo, WIKIDATA_CONCEPTS);
                int correctId = uPage.getLocalId(simple);
                Iterable<LocalLink> inlinks = linkDao.getLinks(simple, correctId, false);
                int numLinks = 0;
                for (LocalLink inlink : inlinks) {
                    numLinks++;
                    if (numLinks>threshold){
                        break;
                    }
                }

                if (numLinks > threshold) {
                    significantGeometries.add(geo);
                    System.out.println(lpDao.getById(simple, correctId).getTitle());

                }
            } catch (Exception e) {
                System.out.println("Could not get inlinks");
            }
        }
        FileOutputStream fos = new FileOutputStream("significantGeo.txt");
        ObjectOutputStream oos = new ObjectOutputStream(fos);

        oos.writeObject(significantGeometries);

        oos.close();


    }

    public void loadSignificantGeometries(File file){

        try {
            FileInputStream fis = new FileInputStream(file);
            ObjectInputStream ois = new ObjectInputStream(fis);
            significantGeometries = (List) ois.readObject();
            ois.close();
        } catch (Exception e) {
            System.out.println("file not found");
        }
    }
}