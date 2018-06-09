package org.lenskit.mooc.nonpers.mean;

import it.unimi.dsi.fastutil.longs.Long2DoubleOpenHashMap;
import org.lenskit.data.dao.DataAccessObject;
import org.lenskit.data.ratings.Rating;
import org.lenskit.inject.Transient;
import org.lenskit.util.io.ObjectStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.*;

/**
 * Provider class that builds the mean rating item scorer, computing item means from the
 * ratings in the DAO.
 */
public class ItemMeanModelProvider implements Provider<ItemMeanModel> {
    /**
     * A logger that you can use to emit debug messages.
     */
    private static final Logger logger = LoggerFactory.getLogger(ItemMeanModelProvider.class);

    /**
     * The data access object, to be used when computing the mean ratings.
     */
    private final DataAccessObject dao;

    /**
     * Constructor for the mean item score provider.
     *
     * <p>The {@code @Inject} annotation tells LensKit to use this constructor.
     *
     * @param dao The data access object (DAO), where the builder will get ratings.  The {@code @Transient}
     *            annotation on this parameter means that the DAO will be used to build the model, but the
     *            model will <strong>not</strong> retain a reference to the DAO.  This is standard procedure
     *            for LensKit models.
     */
    @Inject
    public ItemMeanModelProvider(@Transient DataAccessObject dao) {
        this.dao = dao;
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
        // TODO Set up data structures for computing means

        Map<Long, ArrayList<Object>> movieMap = new HashMap<>();
        Long2DoubleOpenHashMap means = new Long2DoubleOpenHashMap();

        try (ObjectStream<Rating> ratings = dao.query(Rating.class).stream()) {
            for (Rating r: ratings) {

                // TODO process this rating
                Object b = r.getValue();
                //Checking if an earlier entry has been made
               if(movieMap.get(r.getItemId())!=null)
               {
                   ArrayList<Object> rating= movieMap.get(r.getItemId());
                   rating.add(b);
                  //  movieMap.put(r.getItemId(),rating);   //optional
               }
               //If it's a new entry
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

            //Calculating mean for each movie
            for(Long movieID : setOfKeySet) {

                for(Object rating : movieMap.get(movieID)) {
                   mean+=Double.valueOf(rating.toString());
                    count++;
                }
                mean=mean/count;
                means.put(movieID,mean);
                mean=0.0;
                count=0;
            }
        }
        logger.info("computed mean ratings for {} items", means.size());
        return new ItemMeanModel(means);
        // TODO Finalize means to store them in the mean model
    }
}
