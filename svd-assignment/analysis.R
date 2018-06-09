library(dplyr)
install.packages('ggplot2', dep = TRUE)
library(ggplot2)

options(repr.plot.width=5, repr.plot.height=4)

results = read.csv('eval-results.csv')
head(results)


static_results = results %>% filter(is.na(NNbrs))
head(static_results)

static_results=results

ggplot(static_results) +
  aes(x=Algorithm, y=RMSE.ByUser) +
  geom_boxplot()

static_results = results %>% filter(Algorithm=="Popular")
ggplot(static_results) +
  aes(group=NNbrs,x=NNbrs, y=TopN.nDCG) +
  geom_boxplot()

ggplot(static_results) +
  aes(x=Algorithm, y=TopN.nDCG) +
  geom_boxplot()


static_results = results %>% filter(Algorithm=="UserUser")
ggplot(static_results) +
aes(group=NNbrs,x=NNbrs, y=Predict.nDCG) +
  geom_boxplot()

ggplot(static_results) +
  aes(x=Algorithm, y=RMSE.ByRating) +
  geom_boxplot()

ggplot(static_results) +
  aes(x=Bias, y=RMSE.ByRating) +
  geom_boxplot()

ggplot(static_results) +
  aes(x=Algorithm, y=TopN.nDCG) +
  geom_boxplot()

ggplot(static_results) +
  aes(x=Bias, y=TopN.nDCG) +
  geom_boxplot()

ggplot(static_results) +
  aes(x=Algorithm, y=MRR) +
  geom_boxplot()

ggplot(static_results) +
  aes(x=Algorithm, y=MAP) +
  geom_boxplot()

static_results = results %>% filter(Algorithm=="SVD")
ggplot(static_results) +
  aes(group=FeatureCount,x=FeatureCount, y=RMSE.ByRating) +
  geom_boxplot()


