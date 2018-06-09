package org.lenskit.mooc.hybrid;

import it.unimi.dsi.fastutil.longs.LongSet;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;
import org.lenskit.api.ItemScorer;
import org.lenskit.api.Result;
import org.lenskit.api.ResultMap;
import org.lenskit.basic.AbstractItemScorer;
import org.lenskit.bias.BiasModel;
import org.lenskit.bias.UserBiasModel;
import org.lenskit.data.ratings.Rating;
import org.lenskit.data.ratings.RatingSummary;
import org.lenskit.results.Results;
import org.lenskit.util.collections.LongUtils;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.*;

/**
 * Item scorer that does a logistic blend of a subsidiary item scorer and popularity.  It tries to predict
 * whether a user has rated a particular item.
 */
public class LogisticItemScorer extends AbstractItemScorer {
    private final LogisticModel logisticModel;
    private final BiasModel biasModel;
    private final RecommenderList recommenders;
    private final RatingSummary ratingSummary;

    @Inject
    public LogisticItemScorer(LogisticModel model, UserBiasModel bias, RecommenderList recs, RatingSummary rs) {
        logisticModel = model;
        biasModel = bias;
        recommenders = recs;
        ratingSummary = rs;
    }

    @Nonnull
    @Override
    public ResultMap scoreWithDetails(long user, @Nonnull Collection<Long> items) {
        // TODO Implement item scorer
         List<Result> results = new ArrayList<>();


        HashMap<Long,RealVector> cached = new HashMap<>();
        for(Long r : items){
            List<ItemScorer> variables = recommenders.getItemScorers();
            double pop = Math.log(ratingSummary.getItemRatingCount(r));
            double x1 = biasModel.getIntercept() + biasModel.getItemBias(r) + biasModel.getUserBias(user);
            RealVector temp = new ArrayRealVector();
            temp=temp.append(x1);
            temp=temp.append(pop);
            for(ItemScorer it : variables){
                if (it.score(user,r)==null)
                    temp=temp.append(0.0);
                else
                    temp=temp.append(it.score(user,r).getScore()-x1);

            }
            cached.put(r,temp);
        }
        for(Long r : items) {
            double eval = logisticModel.evaluate(1,cached.get(r));
            results.add(Results.create(r,eval));
        }

        return Results.newResultMap(results);
        //throw new UnsupportedOperationException("item scorer not implemented");
    }
}
