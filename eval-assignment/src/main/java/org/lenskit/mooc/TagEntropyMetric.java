package org.lenskit.mooc;

import org.lenskit.LenskitRecommender;
import org.lenskit.api.Recommender;
import org.lenskit.api.Result;
import org.lenskit.api.ResultList;
import org.lenskit.data.dao.DataAccessObject;
import org.lenskit.data.entities.Entity;
import org.lenskit.eval.traintest.AlgorithmInstance;
import org.lenskit.eval.traintest.DataSet;
import org.lenskit.eval.traintest.TestUser;
import org.lenskit.eval.traintest.metrics.MetricColumn;
import org.lenskit.eval.traintest.metrics.MetricResult;
import org.lenskit.eval.traintest.metrics.TypedMetricResult;
import org.lenskit.eval.traintest.recommend.TopNMetric;
import org.lenskit.mooc.cbf.TagData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.text.html.HTMLDocument;
import java.util.*;

/**
 * Metric that measures how long a TopN list actually is.
 */
public class TagEntropyMetric extends TopNMetric<TagEntropyMetric.Context> {
    private static final Logger logger = LoggerFactory.getLogger(TagEntropyMetric.class);

    /**
     * Construct a new tag entropy metric metric.
     */
    public TagEntropyMetric() {
        super(TagEntropyResult.class, TagEntropyResult.class);
    }

    @Nonnull
    @Override
    public MetricResult measureUser(TestUser user, int expectedSize, ResultList recommendations, Context context) {
        if (recommendations == null || recommendations.isEmpty()) {
            return MetricResult.empty();
            // no results for this user.
        }
        int n = recommendations.size();

        // get tag data from the context so we can use it
        DataAccessObject dao = context.getDAO();
        double entropy = 0.0;

        List<Long> movies=recommendations.idList();
        HashSet<String> allTags = new HashSet<>();
        HashMap<Long,HashMap<String,Integer>> tagCountPerMovie=new HashMap<>();
        HashMap<Long, Double> tagMovieCnt = new HashMap<>();
        int j;

        for(int i=0;i<movies.size();i++) {

            HashMap<String, Integer> tagCount = new HashMap<>();
            List<Entity> tags = dao.query(TagData.ITEM_TAG_TYPE).withAttribute(TagData.ITEM_ID, movies.get(i)).get();

            tagMovieCnt.put(movies.get(i),(double)tags.size());
            for (j = 0; j < tags.size(); j++) {
               if(tagCount.get(tags.get(j).get(TagData.TAG))==null){
                    tagCount.put(tags.get(j).get(TagData.TAG),1);
                }
                else{
                    tagCount.put(tags.get(j).get(TagData.TAG),tagCount.get(tags.get(j).get(TagData.TAG))+1);
                }

                //Count no: tags for each movie so movie,tag,count
                allTags.add(tags.get(j).get(TagData.TAG));
            }
            //  prob=tagCount.get(it)/tagCount.size();
                //entropy+=prob*(Math.log(prob)/Math.log(2))*-1;
                tagCountPerMovie.put(movies.get(i),tagCount);

        }




        for(String it:allTags)
        { double prob=0.0;
            for(int i=0;i<movies.size();i++)
            {

                if(tagCountPerMovie.get(movies.get(i)).get(it)==null)
                    continue;
                else{
                    prob+=((double)tagCountPerMovie.get(movies.get(i)).get(it)/(tagMovieCnt.get(movies.get(i))*recommendations.size()));
                }
            }
            entropy+=prob*(Math.log(prob)/Math.log(2))*(-1);
        }

        // TODO Compute the entropy of the movie list
        // You can get a movie's tags with:
        // dao.query(TagData.ITEM_TAG_TYPE).withAttribute(TagData.ITEM_ID, res.getId()).get();
        // Each entity's tag can be retrieved with 'itemTag.get(TagData.TAG)'
        context.addUser(entropy);
        return new TagEntropyResult(entropy);
    }

    @Nullable
    @Override
    public Context createContext(AlgorithmInstance algorithm, DataSet dataSet, Recommender recommender) {
        return new Context((LenskitRecommender) recommender);
    }

    @Nonnull
    @Override
    public MetricResult getAggregateMeasurements(Context context) {
        return new TagEntropyResult(context.getMeanEntropy());
    }

    public static class TagEntropyResult extends TypedMetricResult {
        @MetricColumn("TopN.TagEntropy")
        public final double entropy;

        public TagEntropyResult(double ent) {
            entropy = ent;
        }

    }

    public static class Context {
        private LenskitRecommender recommender;
        private double totalEntropy;
        private int userCount;

        /**
         * Create a new context for evaluating a particular recommender.
         *
         * @param rec The recommender being evaluated.
         */
        public Context(LenskitRecommender rec) {
            recommender = rec;
        }

        /**
         * Get the recommender being evaluated.
         *
         * @return The recommender being evaluated.
         */
        public LenskitRecommender getRecommender() {
            return recommender;
        }

        /**
         * Get the DAO for the current recommender evaluation.
         */
        public DataAccessObject getDAO() {
            return recommender.get(DataAccessObject.class);
        }

        /**
         * Add the entropy for a user to this context.
         *
         * @param entropy The entropy for one user.
         */
        public void addUser(double entropy) {
            totalEntropy += entropy;
            userCount += 1;
        }

        /**
         * Get the average entropy over all users.
         *
         * @return The average entropy over all users.
         */
        public double getMeanEntropy() {
            return totalEntropy / userCount;
        }
    }
}
