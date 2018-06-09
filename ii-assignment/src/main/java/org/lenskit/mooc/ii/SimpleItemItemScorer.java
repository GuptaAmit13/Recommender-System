package org.lenskit.mooc.ii;

import it.unimi.dsi.fastutil.longs.Long2DoubleMap;
import it.unimi.dsi.fastutil.longs.Long2DoubleOpenHashMap;
import org.lenskit.api.Result;
import org.lenskit.api.ResultMap;
import org.lenskit.basic.AbstractItemScorer;
import org.lenskit.data.dao.DataAccessObject;
import org.lenskit.data.entities.CommonAttributes;
import org.lenskit.data.ratings.Rating;
import org.lenskit.results.Results;
import org.lenskit.util.ScoredIdAccumulator;
import org.lenskit.util.TopNScoredIdAccumulator;
import org.lenskit.util.math.Vectors;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.*;

/**
 * @author <a href="http://www.grouplens.org">GroupLens Research</a>
 */
public class SimpleItemItemScorer extends AbstractItemScorer {
    private final SimpleItemItemModel model;
    private final DataAccessObject dao;
    private final int neighborhoodSize;

    @Inject
    public SimpleItemItemScorer(SimpleItemItemModel m, DataAccessObject dao) {
        model = m;
        this.dao = dao;
        neighborhoodSize = 20;
    }

    /**
     * Score items for a user.
     * @param user The user ID.
     * @param items The score vector.  Its key domain is the items to score, and the scores
     *               (rating predictions) should be written back to this vector.
     */
    @Override
    public ResultMap scoreWithDetails(long user, @Nonnull Collection<Long> items) {
        Long2DoubleMap itemMeans = model.getItemMeans();
        Long2DoubleMap ratings = getUserRatingVector(user);


        for(long ItemId : ratings.keySet()){
            ratings.put(ItemId,ratings.get(ItemId)-itemMeans.get(ItemId));
        }


        // TODO Normalize the user's ratings by subtracting the item mean from each one.



        List<Result> results = new ArrayList<>();
        Result r;
        for (long ItemId: items ) {
            // TODO Compute the user's score for each item, add it to results
            Long2DoubleMap neig =  model.getNeighbors(ItemId);



            //Sorting Function
            List<Long> mapKeys = new ArrayList<>(neig.keySet());
            List<Double> mapValues = new ArrayList<>(neig.values());
            Collections.sort(mapValues,Collections.<Double>reverseOrder());
            Collections.sort(mapKeys,Collections.<Long>reverseOrder());

            LinkedHashMap<Long, Double> sortedMap =
                    new LinkedHashMap<>();

            Iterator<Double> valueIt = mapValues.iterator();
            while (valueIt.hasNext()) {
                Double val = valueIt.next();
                Iterator<Long> keyIt = mapKeys.iterator();

                while (keyIt.hasNext()) {
                    Long key = keyIt.next();
                    Double comp1 = neig.get(key);
                    Double comp2 = val;

                    if (comp1.equals(comp2)) {
                        keyIt.remove();
                        sortedMap.put(key, val);
                        break;
                    }
                }
            }


            neig.clear();
            int i=0;
            double sum=0;
            for(long it : sortedMap.keySet()){
                if(i<20 && ratings.get(new Long(it))!=null && it!=ItemId){
                    neig.put(new Long(it),new Double(sortedMap.get(it)));
                    i++;
                    sum+=sortedMap.get(it);
                }
                if(i==20)
                    break;
            }

            double Itemscore = (Vectors.dotProduct(ratings,neig)/sum)+itemMeans.get(ItemId);
                r = Results.create(ItemId,Itemscore);
                results.add(r);
        }

        return Results.newResultMap(results);

    }

    /**
     * Get a user's ratings.
     * @param user The user ID.
     * @return The ratings to retrieve.
     */
    private Long2DoubleOpenHashMap getUserRatingVector(long user) {
        List<Rating> history = dao.query(Rating.class)
                                  .withAttribute(CommonAttributes.USER_ID, user)
                                  .get();

        Long2DoubleOpenHashMap ratings = new Long2DoubleOpenHashMap();
        for (Rating r: history) {
            ratings.put(r.getItemId(), r.getValue());
        }

        return ratings;
    }


}
