(ns rocks.mygiftlist.application
  (:require [com.fulcrologic.fulcro.application :as app]
            [com.fulcrologic.fulcro.rendering.keyframe-render2 :as keyframe-render2]))

(defonce SPA (app/fulcro-app {:optimized-render! keyframe-render2/render!}))
