package org.lenskit.mooc.hybrid;

import com.google.common.base.Preconditions;
import it.unimi.dsi.fastutil.longs.LongSet;
import org.apache.commons.math3.linear.RealVector;
import org.lenskit.api.ItemScorer;
import org.lenskit.api.Result;
import org.lenskit.api.ResultMap;
import org.lenskit.basic.AbstractItemScorer;
import org.lenskit.bias.BiasModel;
import org.lenskit.results.Results;
import org.lenskit.util.collections.LongUtils;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Item scorer that computes a linear blend of two scorers' scores.
 *
 * <p>This scorer takes two underlying scorers and blends their scores.
 */
public class LinearBlendItemScorer extends AbstractItemScorer {
    private final BiasModel biasModel;
    private final ItemScorer leftScorer, rightScorer;
    private final double blendWeight;

    /**
     * Construct a popularity-blending item scorer.
     *
     * @param bias   The baseline bias model to use.
     * @param left   The first item scorer to use.
     * @param right  The second item scorer to use.
     * @param weight The weight to give popularity when ranking.
     */
    @Inject
    public LinearBlendItemScorer(BiasModel bias,
                                 @Left ItemScorer left,
                                 @Right ItemScorer right,
                                 @BlendWeight double weight) {
        Preconditions.checkArgument(weight >= 0 && weight <= 1, "weight out of range");
        biasModel = bias;
        leftScorer = left;
        rightScorer = right;
        blendWeight = weight;
    }

    @Nonnull
    @Override
    public ResultMap scoreWithDetails(long user, @Nonnull Collection<Long> items) {
        List<Result> results = new ArrayList<>();
        for (Long item : items) {
            double baseline = biasModel.getIntercept() + biasModel.getUserBias(user) + biasModel.getItemBias(item);
            double pred = baseline;
            Result left = leftScorer.score(user,item);
            Result right = rightScorer.score(user,item);
            if(left!=null && right!=null)
                pred = baseline + ((1 - blendWeight)*(left.getScore()- baseline)) + ((blendWeight) * (right.getScore() - baseline));
            else if(left==null && right!=null)
                pred = baseline + ((blendWeight)*(right.getScore() - baseline));
            else if(right==null && left!=null)
                pred = baseline + ((1 - blendWeight)*(left.getScore() - baseline));
            results.add(Results.create(item,pred));
        }
        // TODO Compute hybrid scores

        return Results.newResultMap(results);
    }
}
