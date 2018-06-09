package org.lenskit.mooc.uu;

import com.google.common.collect.Maps;
import it.unimi.dsi.fastutil.longs.Long2DoubleMap;
import it.unimi.dsi.fastutil.longs.Long2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2DoubleSortedMap;
import it.unimi.dsi.fastutil.longs.LongSet;
import org.lenskit.api.Result;
import org.lenskit.api.ResultList;
import org.lenskit.api.ResultMap;
import org.lenskit.basic.AbstractItemScorer;
import org.lenskit.data.dao.DataAccessObject;
import org.lenskit.data.entities.CommonAttributes;
import org.lenskit.data.entities.CommonTypes;
import org.lenskit.data.ratings.Rating;
import org.lenskit.results.Results;
import org.lenskit.util.ScoredIdAccumulator;
import org.lenskit.util.TopNScoredIdAccumulator;
import org.lenskit.util.collections.LongUtils;
import org.lenskit.util.math.Scalars;
import org.lenskit.util.math.Vectors;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.*;

/**
 * User-user item scorer.
 * @author <a href="http://www.grouplens.org">GroupLens Research</a>
 */
public class SimpleUserUserItemScorer extends AbstractItemScorer {
    private final DataAccessObject dao;
    private final int neighborhoodSize;

    /**
     * Instantiate a new user-user item scorer.
     * @param dao The data access object.
     */
    @Inject
    public SimpleUserUserItemScorer(DataAccessObject dao) {
        this.dao = dao;
        neighborhoodSize = 30;
    }

    @Nonnull
    @Override
    public ResultMap scoreWithDetails(long user, @Nonnull Collection<Long> items) {
        // TODO Score the items for the user with user-user CF
        Long2DoubleOpenHashMap Ratings=new Long2DoubleOpenHashMap();
        List<Result> results = new ArrayList<>();
        Double sum=0.0;int count=0;

        LongSet userIds=dao.getEntityIds(CommonTypes.USER);

        HashMap<Long,HashMap<Long,Double>> userMeanCentered = new HashMap<>();
        for(Long userId:userIds)
        {
            sum=0.0;
            count=0;
            Ratings=getUserRatingVector(userId);
            for(Long r:Ratings.keySet())
            {
                sum+=Ratings.get(r);
                count++;
            }
            //Double mean = sum/Ratings.size();
            Double mean=sum/count;
            //System.out.println(mean+" :"+userId);
            HashMap<Long,Double> meanCentered=new HashMap<>();
            for(Long r:Ratings.keySet())
            {
                //if((Ratings.get(r))!=null)
                meanCentered.put(r,Ratings.get(r)-mean);
               /* else {
                    meanCentered.put(r, 0.0);
                    System.out.println("Else");
                }*/
                //if(r==134627) System.out.print(meanCentered.get(r));
            }
            userMeanCentered.put(userId,meanCentered);
        }

        HashMap<Long,Double> targetRatings=userMeanCentered.get(user);
        HashMap<Long,Double> correlation=new HashMap<>();
        HashMap<Long,Double> userRatings=new HashMap<>();
        LongSet movieList=dao.getEntityIds(CommonTypes.ITEM);

        Double similarity;
        for(Long userID:userIds)
        {
            Double numerator=0.0;
            Double denominatorTarget=0.0;
            Double denominatorUser=0.0;
            userRatings=userMeanCentered.get(userID);

            for(Long movieId : movieList)
            {

              if(targetRatings.get(movieId)!=null && userRatings.get(movieId)!=null)
                {
                    numerator += targetRatings.get(movieId) * userRatings.get(movieId);
                }
                if(targetRatings.get(movieId)!=null)
                {
                    denominatorTarget+=targetRatings.get(movieId)*targetRatings.get(movieId);
                }
                if(userRatings.get(movieId)!=null)
                {
                    denominatorUser+=userRatings.get(movieId)*userRatings.get(movieId);
                }
            }

            similarity=numerator/(Math.sqrt(denominatorTarget)*Math.sqrt(denominatorUser));
            correlation.put(userID,similarity);
          //  System.out.println(userID+" :"+similarity);
        }

        List<Long> mapKeys = new ArrayList<>(correlation.keySet());
        List<Double> mapValues = new ArrayList<>(correlation.values());
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
                Double comp1 = correlation.get(key);
                Double comp2 = val;

                if (comp1.equals(comp2)) {
                    keyIt.remove();
                    sortedMap.put(key, val);
                    break;
                }
            }
        }

        count=0;
        sum=0.0;
        Ratings=getUserRatingVector(user);
        for(Long r:Ratings.keySet())
        {
            sum+=Ratings.get(r);
            count++;
        }
        Double userMean=sum/count;
       // System.out.println(userMean);


        //int i=0;
        Iterator iterator=items.iterator();
        while(iterator.hasNext())
        {
            HashMap<Long,Double> top30Correlation=new HashMap<>();
            int i=0;
            Double numerator=0.0;
            Double denominator=0.0;
            Long movieID=Long.parseLong(iterator.next().toString());
           // System.out.println("Hello "+movieID);
            for(Long l:sortedMap.keySet())
            {
                if(i==0){
                    i++;
                }
                else if(i<=30 && userMeanCentered.get(l).get(movieID)!=null) {
                    top30Correlation.put(l,sortedMap.get(l));
                   //  System.out.println(l + " :" + top30Correlation.get(l));
                    i++;
                }
                else if(i>30){
                    break;
                }
                else ;
            }


            Result r;
            i=0;
            if(top30Correlation.size()>2) {
                for (Long neighbor : top30Correlation.keySet()) {
                  //  System.out.println(i++);
                   // System.out.println(neighbor + " :" + top30Correlation.get(neighbor));
                    Double meanCenteredRating = userMeanCentered.get(neighbor).get(movieID);
                    Double corr = top30Correlation.get(neighbor);
                   if (meanCenteredRating != null && corr >0 ) {
                     //  System.out.println("Neighbor " + neighbor + " Mean " + meanCenteredRating);
                       // Double corr = top30Correlation.get(neighbor);
                     //  System.out.println("Corr " + corr);
                        numerator += meanCenteredRating * corr;
                        denominator +=Math.abs(corr);
                    }
                }
            }
            Double pred=userMean+(numerator/denominator);
            r = Results.create(movieID,pred);
            results.add(r);
            //System.out.println(pred);

           // top30Correlation.clear();
        }

        return Results.newResultMap(results) ;
    }

    /**
     * Get a user's rating vector.
     * @param user The user ID.
     * @return The rating vector, mapping item IDs to the user's rating
     *         for that item.
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
