package org.lenskit.mooc.hybrid;

import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;
import org.grouplens.lenskit.iterative.IterationCount;
import org.lenskit.api.ItemScorer;
import org.lenskit.api.Result;
import org.lenskit.bias.BiasModel;
import org.lenskit.bias.UserBiasModel;
import org.lenskit.data.ratings.Rating;
import org.lenskit.data.ratings.RatingSummary;
import org.lenskit.data.ratings.Ratings;
import org.lenskit.inject.Transient;
import org.lenskit.util.ProgressLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.*;

/**
 * Trainer that builds logistic models.
 */
public class LogisticModelProvider implements Provider<LogisticModel> {
    private static final Logger logger = LoggerFactory.getLogger(LogisticModelProvider.class);
    private static final double LEARNING_RATE = 0.00005;
    private static final int ITERATION_COUNT = 100;

    private final LogisticTrainingSplit dataSplit;
    private final BiasModel baseline;
    private final RecommenderList recommenders;
    private final RatingSummary ratingSummary;
    private final int parameterCount;
    private final Random random;

    @Inject
    public LogisticModelProvider(@Transient LogisticTrainingSplit split,
                                 @Transient UserBiasModel bias,
                                 @Transient RecommenderList recs,
                                 @Transient RatingSummary rs,
                                 @Transient Random rng) {
        dataSplit = split;
        baseline = bias;
        recommenders = recs;
        ratingSummary = rs;
        parameterCount = 1 + recommenders.getRecommenderCount() + 1;
        random = rng;
    }

    @Override
    public LogisticModel get() {
        List<ItemScorer> scorers = recommenders.getItemScorers();
        double intercept = 0;
        double[] params = new double[parameterCount];
        LogisticModel current = LogisticModel.create(intercept, params);
        HashMap<Rating,RealVector> cached = new HashMap<>();


        //cache score
        List<Rating> tunedratings = dataSplit.getTuneRatings();

        /*
        for(Rating r : tunedratings){
            List<ItemScorer> variables = recommenders.getItemScorers();
            double pop = Math.log(ratingSummary.getItemRatingCount(r.getItemId()));
            double x1 = baseline.getIntercept() + baseline.getItemBias(r.getItemId()) + baseline.getUserBias(r.getUserId());
            RealVector temp = new ArrayRealVector();
            temp=temp.append(x1);
            temp=temp.append(pop);
            for(ItemScorer it : variables){
                if (it.score(r.getUserId(),r.getItemId())==null)
                    temp=temp.append(0.0);
                else
                    temp=temp.append(it.score(r.getUserId(),r.getItemId()).getScore()-x1);
            }
            cached.put(r,temp);
        }


        for(int i=0;i<=ITERATION_COUNT;i++){
            Collections.shuffle(tunedratings);
            for(Rating r : tunedratings){

                double eval = current.evaluate(-r.getValue(),cached.get(r));
                intercept += r.getValue() * eval * LEARNING_RATE;
                for(int j=0;j<parameterCount;j++)
                    params[j]+=cached.get(r).getEntry(j)*r.getValue() * eval * LEARNING_RATE;
                current = LogisticModel.create(intercept, params);
            }
        }
        */// TODO Implement model training
        for(int i=0;i<=ITERATION_COUNT;i++) {
            Collections.shuffle(tunedratings);
            for (Rating r : tunedratings) {
                if(cached.get(r)==null) {
                    List<ItemScorer> variables = recommenders.getItemScorers();
                    double pop = Math.log(ratingSummary.getItemRatingCount(r.getItemId()));
                    double x1 = baseline.getIntercept() + baseline.getItemBias(r.getItemId()) + baseline.getUserBias(r.getUserId());
                    RealVector temp = new ArrayRealVector();
                    temp = temp.append(x1);
                    temp = temp.append(pop);
                    for (ItemScorer it : variables) {
                        if (it.score(r.getUserId(), r.getItemId()) == null)
                            temp = temp.append(0.0);
                        else
                            temp = temp.append(it.score(r.getUserId(), r.getItemId()).getScore() - x1);
                    }
                    cached.put(r,temp);
                }
                else {
                    double eval = current.evaluate(-r.getValue(), cached.get(r));
                    intercept += r.getValue() * eval * LEARNING_RATE;
                    for (int j = 0; j < parameterCount; j++)
                        params[j] += cached.get(r).getEntry(j) * r.getValue() * eval * LEARNING_RATE;
                    current = LogisticModel.create(intercept, params);
                }
            }
        }
        return current;
    }

}
