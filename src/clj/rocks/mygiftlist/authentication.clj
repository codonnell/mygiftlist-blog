(ns rocks.mygiftlist.authentication
  (:require [rocks.mygiftlist.config :as config]
            [integrant.core :as ig])
  (:import [java.net URL]
           [com.auth0.jwk GuavaCachedJwkProvider UrlJwkProvider]
           [com.auth0.jwt.interfaces RSAKeyProvider]
           [com.auth0.jwt JWT]
           [com.auth0.jwt.algorithms Algorithm]
           [com.auth0.jwt.exceptions JWTVerificationException]))

(defn create-key-provider [url]
  (let [provider (-> url
                   (URL.)
                   (UrlJwkProvider.)
                   (GuavaCachedJwkProvider.))]
    (reify RSAKeyProvider
      (getPublicKeyById [_ key-id]
        (-> provider
          (.get key-id)
          (.getPublicKey)))
      (getPrivateKey [_] nil)
      (getPrivateKeyId [_] nil))))

(defn verify-token
  "Given a key-provider created by `create-key-provider`, an issuer,
  an audience, and a jwt, decodes the jwt and returns it if the jwt is
  valid. Returns nil if the jwt is invalid."
  [key-provider {:keys [issuer audience]} token]
  (let [algorithm (Algorithm/RSA256 key-provider)
        verifier (-> algorithm
                   (JWT/require)
                   (.withIssuer (into-array String [issuer]))
                   (.withAudience (into-array String [audience]))
                   (.build))]
    (try
      (let [decoded-jwt (.verify verifier token)]
        {:iss (.getIssuer decoded-jwt)
         :sub (.getSubject decoded-jwt)
         :aud (vec (.getAudience decoded-jwt))
         :iat (.toInstant (.getIssuedAt decoded-jwt))
         :exp (.toInstant (.getExpiresAt decoded-jwt))
         :azp (.asString (.getClaim decoded-jwt "azp"))
         :scope (.asString (.getClaim decoded-jwt "scope"))})
      (catch JWTVerificationException _
        nil))))

(defn- get-token [req]
  (when-let [header (get-in req [:headers "authorization"])]
    (second (re-find #"^Bearer (.+)" header))))

(defn wrap-jwt
  "Middleware that verifies and adds claim data to a request based on
  a bearer token in the header.

  If a bearer token is found in the authorization header, attempts to
  verify it. If verification succeeds, adds the token's claims to the
  request under the `::claims` key. If verification fails, leaves the
  request unchanged."
  [handler key-provider expected-claims]
  (fn [req]
    (let [token (get-token req)
          claims (when token
                   (verify-token key-provider expected-claims token))]
      (handler (cond-> req
                 claims (assoc ::claims claims))))))

(defmethod ig/init-key ::wrap-jwt
  [_ {::config/keys [config]}]
  (fn [handler]
    (wrap-jwt handler
      (create-key-provider
        (config/jwk-endpoint config))
      {:issuer (config/jwt-issuer config)
       :audience (config/jwt-audience config)})))
