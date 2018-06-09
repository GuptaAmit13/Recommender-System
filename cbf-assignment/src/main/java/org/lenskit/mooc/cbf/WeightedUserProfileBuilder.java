package org.lenskit.mooc.cbf;

import org.lenskit.data.ratings.Rating;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Build a user profile from all positive ratings.
 */
public class WeightedUserProfileBuilder implements UserProfileBuilder {
    /**
     * The tag model, to get item tag vectors.
     */
    private final TFIDFModel model;

    @Inject
    public WeightedUserProfileBuilder(TFIDFModel m) {
        model = m;
    }

    @Override
    public Map<String, Double> makeUserProfile(@Nonnull List<Rating> ratings) {
        // Create a new vector over tags to accumulate the user profile
        Map<String,Double> profile = new HashMap<>();
        Double sum=0.0;
        int count=0;
        for(Rating r:ratings)
        {
           Long userId= r.getUserId();
           Double rating=r.getValue();
           sum+=rating;
           count++;
        }
        sum=sum/count;
        for(Rating r:ratings) {
            Map<String, Double> temp = new HashMap<>();
                temp = model.getItemVector(r.getItemId());
                Set<String> stringSet = temp.keySet();
                for (String s : stringSet) {
                    if(temp.get(s)==null)
                        temp.put(s,0.0);
                    if (profile.get(s) == null) {
                        profile.put(s, (r.getValue()-sum)*temp.get(s));
                    } else {
                        profile.put(s, profile.get(s) + (r.getValue()-sum)*temp.get(s));
                    }
                }
                // TODO Get this item's vector and add it to the user's profile
        }
        // TODO Normalize the user's ratings
        // TODO Build the user's weighted profile


        // The profile is accumulated, return it.
        return profile;
    }
}
