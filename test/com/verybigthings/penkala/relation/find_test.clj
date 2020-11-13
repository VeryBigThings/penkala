(ns com.verybigthings.penkala.relation.find-test
  (:require [clojure.test :refer :all]
            [com.verybigthings.penkala.db :refer [query query-one prettify-sql execute!]]
            [com.verybigthings.penkala.relation :as r]
            [com.verybigthings.penkala.test-helpers :as th :refer [db-uri *db*]]
            [com.verybigthings.penkala.rel :as rel]
            [com.verybigthings.penkala.rel2 :as rel2]
            [com.verybigthings.penkala.statement.select2 :as sel]
            [next.jdbc :as jdbc]
            [clojure.spec.alpha :as s]
            [com.verybigthings.penkala.util.decompose :refer [decompose infer-schema]]))

(s/check-asserts true)

(use-fixtures :once (partial th/reset-db-fixture "data-all"))

(deftest it-returns-all-records-by-default
  (let [res (query db-uri *db* :relation/products)]
    (is (= 4 (count res)))))

(deftest it-finds-by-numeric-id
  (let [products (:relation/products *db*)
        res (query-one db-uri *db* (-> products (r/where {:id 1})))]
    (is (map? res))
    (is (= 1 (:id res)))))

(deftest it-finds-by-uuid-id
  (let [orders (:relation/orders *db*)
        order-1 (query-one db-uri *db* orders)
        order-id (:id order-1)
        order-2 (query-one db-uri *db* (-> orders (r/where {:id order-id})))]
    (is (map? order-1))
    (is (map? order-2))
    (is (= order-1 order-2))))

(deftest it-finds-by-multiple-conditions-1
  (let [products (:relation/products *db*)
        res-1 (query-one db-uri *db* (-> products (r/where [:and {:id 2 :name "Product 2"}
                                                            [:and [:or [:and {}]]]
                                                            [:or {:in_stock false} [:and {"price >" 12}]]])))
        res-2 (query-one db-uri *db* (-> products (r/where [:and {:id 2 :name "Product 2"}
                                                            [:or {:in_stock false} {"price >" 12}]])))]
    (is (= 2 (:id res-1) (:id res-2)))
    (is (= res-1 res-2))))


#_(def db-spec
    [{:schema "public",
      :is_insertable_into true,
      :fk_origin_columns nil,
      :pk ["id"],
      :parent nil,
      :columns ["body" "id" "search"],
      :name "uuid_docs",
      :fk_dependent_columns nil,
      :fk_origin_schema nil,
      :fk_origin_name nil,
      :fk nil}
     {:schema "public",
      :is_insertable_into true,
      :fk_origin_columns nil,
      :pk ["id"],
      :parent nil,
      :columns
      ["created_at"
       "description"
       "id"
       "in_stock"
       "name"
       "price"
       "specs"
       "tags"],
      :name "products",
      :fk_dependent_columns nil,
      :fk_origin_schema nil,
      :fk_origin_name nil,
      :fk nil}
     {:schema "public",
      :is_insertable_into true,
      :fk_origin_columns nil,
      :pk ["id"],
      :parent nil,
      :columns ["id" "notes" "ordered_at" "product_id" "user_id"],
      :name "orders",
      :fk_dependent_columns nil,
      :fk_origin_schema nil,
      :fk_origin_name nil,
      :fk nil}
     {:schema "public",
      :is_insertable_into true,
      :fk_origin_columns nil,
      :pk ["id"],
      :parent nil,
      :columns ["body" "id" "search"],
      :name "docs",
      :fk_dependent_columns nil,
      :fk_origin_schema nil,
      :fk_origin_name nil,
      :fk nil}
     {:schema "public",
      :is_insertable_into true,
      :fk_origin_columns nil,
      :pk ["Id"],
      :parent nil,
      :columns ["Email" "Id" "Name" "search"],
      :name "Users",
      :fk_dependent_columns nil,
      :fk_origin_schema nil,
      :fk_origin_name nil,
      :fk nil}])

#_(deftest testing1
  (let [products (rel2/spec->relation {:schema "public",
                                       :is_insertable_into true,
                                       :fk_origin_columns nil,
                                       :pk ["id"],
                                       :parent nil,
                                       :columns ["created_at" "description" "id" "in_stock" "name" "price" "specs" "tags"],
                                       :name "products",
                                       :fk_dependent_columns nil,
                                       :fk_origin_schema nil,
                                       :fk_origin_name nil,
                                       :fk nil})
        orders (rel2/spec->relation {:schema "public",
                                     :is_insertable_into true,
                                     :fk_origin_columns nil,
                                     :pk ["id"],
                                     :parent nil,
                                     :columns ["id" "notes" "ordered_at" "product_id" "user_id"],
                                     :name "orders",
                                     :fk_dependent_columns nil,
                                     :fk_origin_schema nil,
                                     :fk_origin_name nil,
                                     :fk nil})
        users (rel2/spec->relation {:schema "public",
                                    :is_insertable_into true,
                                    :fk_origin_columns nil,
                                    :pk ["Id"],
                                    :parent nil,
                                    :columns ["Email" "Id" "Name" "search"],
                                    :name "Users",
                                    :fk_dependent_columns nil,
                                    :fk_origin_schema nil,
                                    :fk_origin_name nil,
                                    :fk nil})
        computed-rel (rel2/spec->relation {:columns ["foo" "bar"]
                                           :name (str (gensym "rel_"))
                                           :query ["SELECT foo, bar FROM (VALUES ('foo1', 'bar1'), ('foo2', 'bar2')) AS q (foo, bar)"]})
        computed-rel2 (rel2/spec->relation {:columns ["foo" "qux"]
                                           :name (str (gensym "rel_"))
                                           :query ["SELECT foo, qux FROM (VALUES ('foo1', 'qux1'), ('foo2', 'qux2')) AS q (foo, qux)"]})
        rel      (-> products
                   ;;(rel2/extend-with-window :window-sum [:sum :id] nil [:id])
                   (rel2/rename :name :foo)
                   (rel2/lock :share)
                   ;;(rel2/where [:parent-scope [:and [:= :id 1] [:= :id 2]]])
                   ;;(rel2/where [:= :id 1])
                   #_(rel2/extend-with-aggregate :count-products :count 1)
                   ;;(rel2/select [:id :count-products])
                   ;;(rel2/only)
                   ;;(rel2/distinct [:name (rel2/column :id)])
                   ;;(rel2/distinct false)
                   #_(rel2/join
                     :left
                     (rel2/join orders :left
                       (-> users
                         (rel2/alias :foo-bar-users)
                         (rel2/extend :upper-name [:upper :name])
                         (rel2/extend-with-aggregate :count :count 1)
                         (rel2/extend :lower-name [:lower :upper-name])
                         (rel2/rename :lower-name :ln)
                         (rel2/having [:< :count 1]))
                       :users [:= :user-id :users/id])
                     :orders
                     [:or true [:is-not-false [:not false]] [:foo :bar] [:and [:is-true true] [:= :id :orders/product-id]]])
                   #_(rel2/select [:id])

                   #_(rel2/where [:in :id (-> products
                                          (rel2/select [:id])
                                          (rel2/where [:= :id 1]))])
                   ;;(rel2/where [:= :orders.users/ln "A TEST USER"])

                   ;;(rel2/rename :name :product-name)
                   ;;(rel2/extend :upper-product-name [:upper :product-name])
                   ;;(rel2/rename :upper-product-name :upn)
                   ;;(rel2/extend :lpn [:lower :upn])
                   ;;(rel2/order-by [[:lpn :desc]])
                   ;;(rel2/select [:lpn])
                   ;;(rel2/where [:and [:= :upn (rel2/param :product-name)] [:= :id (rel2/param :product-id)]])
                   ;;(rel2/offset 1)
                   ;;(rel2/limit 2)
                   )
        #_#_p1 (-> products
             (rel2/select [:id :name])
             (rel2/join :left (rel2/select orders [:id :product-id]) :orders [:= :id :orders/product-id])
             (rel2/where [:or [:= :id 1] [:= :id 2]]))
        #_#_p2 (-> products
             (rel2/select [:id :name])
             (rel2/join :left (rel2/select orders [:id :product-id]) :orders [:= :id :orders/product-id])
             (rel2/where [:or [:= :id 2] [:= :id 3]]))
        #_#_p1-p2 (rel2/except p1 p2)
        #_#_pp (-> products
             (rel2/join :left p1-p2 :pp [:= :id :pp/id]))
        p-with-parent (rel2/with-parent products products)
        ptest (-> products
                (rel2/extend :pname (-> p-with-parent
                                      (rel2/select [:name])
                                      (rel2/where [:= :id [:parent-scope :id]])))
                (rel2/select [:id :pname]))

        ptest2 (-> products
                 (rel2/select [:id :name])
                 (rel2/join :inner-lateral
                   (-> orders
                     (rel2/with-parent products)
                     (rel2/extend-with-aggregate :serialized [:json-agg [:json-build-object (rel2/quoted-literal :id) :id, (rel2/quoted-literal :user-id), :user-id]])
                     (rel2/select [:serialized :product-id])
                     (rel2/where [:= :product-id [:parent-scope :id]]))
                   :orders
                   [:= :id :orders/product-id]))

        ptest3 (-> products
                 (rel2/where [:fragment (fn [env rel [[query1 & params1]]] [(str query1 " = 2") params1]) :id]))]
    ;;   (println (jdbc/execute! db-uri [(rel/to-sql rel)]))
    ;;(clojure.pprint/pprint ptest3)
    ;;(println (prettify-sql (first (sel/format-query {} (rel2/join orders :left users :users [:= :user-id :users/id]) {:product-name "PRODUCT 1" :product-id 1}))))
    ;;(println (sel/format-query {} (rel2/join computed-rel :inner computed-rel2 :c2 [:= :foo :c2/foo]) {}))
    (println (prettify-sql (first (sel/format-query {} rel {:product-name "PRODUCT 1" :product-id 1}))))
    (println (first (sel/format-query {} rel {:product-name "PRODUCT 1" :product-id 1})))
    ;;(println (-> (rel2/get-select-query p1-p2 {}) first        prettify-sql ))
    ;;(println (sel/format-query {} rel {:product-name "PRODUCT 1" :product-id 1}))
    ;;(println (jdbc/execute! db-uri (sel/format-query {} rel {})))
    (is false)))

#_(deftest testing1
  (let [products (rel2/spec->relation {:schema "public",
                                       :is_insertable_into true,
                                       :fk_origin_columns nil,
                                       :pk ["id"],
                                       :parent nil,
                                       :columns ["created_at" "description" "id" "in_stock" "name" "price" "specs" "tags"],
                                       :name "products",
                                       :fk_dependent_columns nil,
                                       :fk_origin_schema nil,
                                       :fk_origin_name nil,
                                       :fk nil})
        orders (rel2/spec->relation {:schema "public",
                                     :is_insertable_into true,
                                     :fk_origin_columns nil,
                                     :pk ["id"],
                                     :parent nil,
                                     :columns ["id" "notes" "ordered_at" "product_id" "user_id"],
                                     :name "orders",
                                     :fk_dependent_columns nil,
                                     :fk_origin_schema nil,
                                     :fk_origin_name nil,
                                     :fk nil})
        users (rel2/spec->relation {:schema "public",
                                    :is_insertable_into true,
                                    :fk_origin_columns nil,
                                    :pk ["Id"],
                                    :parent nil,
                                    :columns ["Email" "Id" "Name" "search"],
                                    :name "Users",
                                    :fk_dependent_columns nil,
                                    :fk_origin_schema nil,
                                    :fk_origin_name nil,
                                    :fk nil})
        orders-users  (rel2/join orders :left users :users [:= :user-id :users/id])
        products-orders-users (-> products (rel2/join :left orders-users :orders [:= :id :orders/product-id]))]
    (infer-schema products-orders-users)
    ;;(clojure.pprint/pprint (infer-schema products-orders-users))
    ;;(println (prettify-sql (first (sel/format-query {} products-orders-users {}))))
    ;;(println (first (sel/format-query {} products-orders-users {})))
    (decompose (infer-schema products-orders-users) (execute! db-uri (sel/format-query {} products-orders-users {})))
    (clojure.pprint/pprint (decompose (infer-schema products-orders-users) (execute! db-uri (sel/format-query {} products-orders-users {}))))
    (is false)))