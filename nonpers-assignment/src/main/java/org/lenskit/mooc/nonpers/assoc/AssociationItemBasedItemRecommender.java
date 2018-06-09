package org.lenskit.mooc.nonpers.assoc;

import it.unimi.dsi.fastutil.longs.LongSet;
import org.lenskit.api.Result;
import org.lenskit.api.ResultList;
import org.lenskit.basic.AbstractItemBasedItemRecommender;
import org.lenskit.results.Results;
import org.lenskit.util.collections.LongUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.*;

/**
 * An item-based item scorer that uses association rules.
 */
public class AssociationItemBasedItemRecommender extends AbstractItemBasedItemRecommender {
    private static final Logger logger = LoggerFactory.getLogger(AssociationItemBasedItemRecommender.class);
    private final AssociationModel model;

    /**
     * Construct the item scorer.
     *
     * @param m The association rule model.
     */
    @Inject
    public AssociationItemBasedItemRecommender(AssociationModel m) {
        model = m;
    }

    @Override
    public ResultList recommendRelatedItemsWithDetails(Set<Long> basket, int n, @Nullable Set<Long> candidates, @Nullable Set<Long> exclude) {
        LongSet items;
        if (candidates == null) {
            items = model.getKnownItems();
        } else {
            items = LongUtils.asLongSet(candidates);
        }

        if (exclude != null) {
            items = LongUtils.setDifference(items, LongUtils.asLongSet(exclude));
        }

        if (basket.isEmpty()) {
            return Results.newResultList();
        } else if (basket.size() > 1) {
            logger.warn("Reference set has more than 1 item, picking arbitrarily.");
        }

        long refItem = basket.iterator().next();

        return recommendItems(n, refItem, items);
    }

    /**
     * Recommend items with an association rule.
     * @param n The number of recommendations to produce.
     * @param refItem The reference item.
     * @param candidates The candidate items (set of items that can possibly be recommended).
     * @return The list of results.
     */
    private ResultList recommendItems(int n, long refItem, LongSet candidates) {
        List<Result> results = new ArrayList<>();

        HashMap <Long,Double> sortedAssc = new HashMap<>();
        Iterator<Long> it =  candidates.iterator();
        while(it.hasNext()){
            Long ymovi = new Long (it.next());
            sortedAssc.put(ymovi,model.getItemAssociation(refItem,ymovi));
        }


        //Sorting the values
        List<Long> mapKeys = new ArrayList<>(sortedAssc.keySet());
        List<Double> mapValues = new ArrayList<>(sortedAssc.values());
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
                Double comp1 = sortedAssc.get(key);
                Double comp2 = val;

                if (comp1.equals(comp2)) {
                    keyIt.remove();
                    sortedMap.put(key, val);
                    break;
                }
            }
        }

        int i=0;
        Set<Long> setOfKeySet = sortedMap.keySet();
        Result r ;

        for (Long movieID : setOfKeySet) {
            if (model.hasItem(movieID )&& i++<n) {
                r = Results.create(movieID,sortedMap.get(movieID));
                results.add(r);
            }
        }

        // TODO Compute the n highest-scoring items from candidates

        return Results.newResultList(results);
    }
}
