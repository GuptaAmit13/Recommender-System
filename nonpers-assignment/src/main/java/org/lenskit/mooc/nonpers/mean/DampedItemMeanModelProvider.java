package org.lenskit.mooc.nonpers.mean;

import it.unimi.dsi.fastutil.longs.Long2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import org.lenskit.baseline.MeanDamping;
import org.lenskit.data.dao.DataAccessObject;
import org.lenskit.data.ratings.Rating;
import org.lenskit.inject.Transient;
import org.lenskit.util.io.ObjectStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Provider class that builds the mean rating item scorer, computing damped item means from the
 * ratings in the DAO.
 */
public class DampedItemMeanModelProvider implements Provider<ItemMeanModel> {
    /**
     * A logger that you can use to emit debug messages.
     */
    private static final Logger logger = LoggerFactory.getLogger(DampedItemMeanModelProvider.class);

    /**
     * The data access object, to be used when computing the mean ratings.
     */
    private final DataAccessObject dao;
    /**
     * The damping factor.
     */
    private final double damping;

    /**
     * Constructor for the mean item score provider.
     *
     * <p>The {@code @Inject} annotation tells LensKit to use this constructor.
     *
     * @param dao The data access object (DAO), where the builder will get ratings.  The {@code @Transient}
     *            annotation on this parameter means that the DAO will be used to build the model, but the
     *            model will <strong>not</strong> retain a reference to the DAO.  This is standard procedure
     *            for LensKit models.
     * @param damping The damping factor for Bayesian damping.  This is number of fake global-mean ratings to
     *                assume.  It is provided as a parameter so that it can be reconfigured.  See the file
     *                {@code damped-mean.groovy} for how it is used.
     */
    @Inject
    public DampedItemMeanModelProvider(@Transient DataAccessObject dao,
                                       @MeanDamping double damping) {
        this.dao = dao;
        this.damping = damping;
    }

    /**
     * Construct an item mean model.
     *
     * <p>The {@link Provider#get()} method constructs whatever object the provider class is intended to build.</p>
     *
     * @return The item mean model with mean ratings for all items.
     */
    @Override
    public ItemMeanModel get() {

        Map<Long, ArrayList<Object>> movieMap = new HashMap<>();
        Long2DoubleOpenHashMap means = new Long2DoubleOpenHashMap();
        Double globalMean;
        // TODO Compute damped means

        try (ObjectStream<Rating> ratings = dao.query(Rating.class).stream()) {
            Double sum = 0.0;
            int count = 0;
           //Calculating global mean
            for (Rating r : ratings) {
                Double b = r.getValue();
                sum += b;
                count++;
            }
             globalMean = sum / count;
           // System.out.println(globalMean);
        }
            try(ObjectStream<Rating>  ratings = dao.query(Rating.class).stream()){

                for (Rating r: ratings) {

                    // this loop will run once for each rating in the data set
                    // TODO process this rating
                    Object b = r.getValue();
                    //Checking if the entry is already added
                    if(movieMap.get(r.getItemId())!=null)
                    {
                        ArrayList<Object> rating= movieMap.get(r.getItemId());
                        rating.add(b);
                        //  movieMap.put(r.getItemId(),rating);   //optional
                    }
                    //If a new entry
                    else
                    {
                        ArrayList<Object> rating= new ArrayList<>();
                        rating.add(b);
                        movieMap.put(r.getItemId(),rating);
                    }
                }

                Set<Long> setOfKeySet = movieMap.keySet();
                Double mean =0.0;
               int count=0;
                for(Long movieID : setOfKeySet) {
                    for(Object rating : movieMap.get(movieID)) {
                        mean+=Double.valueOf(rating.toString());
                        count++;
                    }
                    mean=(mean + damping*globalMean)/(count+damping);
                    means.put(movieID,mean);
                    mean=0.0;
                    count=0;
                }

            return new ItemMeanModel(means);
                // TODO Remove the line below when you have finished
            //   throw new UnsupportedOperationException("damped mean not implemented");
        }
    }

}
