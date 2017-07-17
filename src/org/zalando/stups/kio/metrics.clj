(ns org.zalando.stups.kio.metrics
  (:require [metrics.meters :as meters]
            [com.stuartsierra.component :as component]))

(defn mark-deprecation [component metric]
  (meters/mark! (metric component)))

(defrecord DeprecationMetrics [metrics]
  component/Lifecycle

  (start [component]
    (let [{:keys [metrics-registry]} metrics]
      (assoc component
        :deprecation-versions-get (meters/meter metrics-registry "deprecation.versions.get")
        :deprecation-version-get (meters/meter metrics-registry "deprecation.version.get")
        :deprecation-version-put (meters/meter metrics-registry "deprecation.version.put")
        :deprecation-application-approvals-get (meters/meter metrics-registry "deprecation.application-approvals.get")
        :deprecation-version-approvals-get (meters/meter metrics-registry "deprecation.version-approvals.get")
        :deprecation-version-approvals-put (meters/meter metrics-registry "deprecation.version-approvals.put"))))
  (stop [component]
    component))
