import org.lenskit.api.ItemScorer
import org.lenskit.basic.PopularityRankItemScorer
import org.lenskit.bias.*

import org.grouplens.lenskit.vectors.similarity.CosineVectorSimilarity
import org.grouplens.lenskit.vectors.similarity.PearsonCorrelation
import org.grouplens.lenskit.vectors.similarity.VectorSimilarity

import org.lenskit.api.ItemScorer
import org.lenskit.bias.BiasModel
import org.lenskit.bias.ItemBiasModel
import org.lenskit.baseline.BaselineScorer

import org.lenskit.bias.UserBiasModel
import org.lenskit.knn.NeighborhoodSize
import org.lenskit.knn.item.ItemItemScorer
import org.lenskit.knn.item.model.ItemItemModel
import org.lenskit.knn.user.UserUserItemScorer
import org.lenskit.transform.normalize.BiasUserVectorNormalizer
import org.lenskit.transform.normalize.MeanCenteringVectorNormalizer
import org.lenskit.transform.normalize.UserVectorNormalizer
import org.lenskit.transform.normalize.VectorNormalizer
import org.lenskit.mooc.cbf.LuceneItemItemModel

algorithm("GlobalMean") {
    // score items by the global mean
    bind ItemScorer to BiasItemScorer
    bind BiasModel to GlobalBiasModel
    // recommendation is meaningless for this algorithm
    bind ItemRecommender to null
}
algorithm("Popular") {
    // score items by their popularity
    bind ItemScorer to PopularityRankItemScorer
    // rating prediction is meaningless for this algorithm
    bind RatingPredictor to null
}
algorithm("ItemMean") {
    // score items by their mean rating
    bind ItemScorer to BiasItemScorer
    bind BiasModel to ItemBiasModel
}
algorithm("PersMean") {
    bind ItemScorer to BiasItemScorer
    bind BiasModel to UserItemBiasModel
}
