(ns dynapath.util-test
  (:use clojure.test
        dynapath.util
        [dynapath.dynamic-classpath :only [DynamicClasspath]])
  (:import (java.net URL URLClassLoader)))

(deftype Frobble [])

(def ^:dynamic *url-cl*)
(def ^:dynamic *basic-cl*)

(let [urls [(URL. "http://ham.biscuit")]
      all-urls (conj urls (URL. "http://gravy.biscuit"))]
  
  (use-fixtures :each
    (fn [f]
      (binding [*url-cl* (URLClassLoader. (into-array urls) nil)
                *basic-cl* (proxy [ClassLoader] [])]
        (f))))
  
  (deftest classpath-urls-should-work-for-a-readable-classloader
    (is (= urls (classpath-urls *url-cl*))))

  (deftest classpath-urls-should-work-for-a-non-readable-classloader
    (is (nil? (classpath-urls *basic-cl*))))

  (deftest all-classpath-urls-should-work-for-a-parent-with-the-urls
    (is (= urls (all-classpath-urls (proxy [ClassLoader] [*url-cl*])))))

  (deftest all-classpath-urls-should-order-urls-properly
    (is (= all-urls (all-classpath-urls (URLClassLoader. (into-array [(last all-urls)]) *url-cl*)))))

  (deftest all-classpath-urls-should-use-the-baseLoader-when-called-with-a-zero-arity
    (add-classpath-url (clojure.lang.RT/baseLoader) (first urls))
    (= (first urls) (last (all-classpath-urls))))
  
  (deftest add-classpath-url-should-work-for-an-addable-classpath
    (is (add-classpath-url *url-cl* (last all-urls)))
    (is (= all-urls (classpath-urls *url-cl*))))

  (deftest add-classpath-url-should-work-for-an-non-addable-classpath
    (is (nil? (add-classpath-url *basic-cl* (last all-urls))))
    (is (nil? (classpath-urls *basic-cl*))))

  (deftest addable-classpath?-should-work
    (let [frobble (Frobble.)]
      (is (not (addable-classpath? frobble)))
      (extend-type Frobble
        DynamicClasspath
        (can-add? [_] true))
      (is (addable-classpath? frobble))
      (extend-type Frobble
        DynamicClasspath
        (can-add? [_] false))
      (is (not (addable-classpath? frobble)))))

  (deftest readable-classpath?-should-work
    (let [frobble (Frobble.)]
      (extend-type Frobble
        DynamicClasspath
        (can-read? [_] false))
      (is (not (readable-classpath? frobble)))
      (extend-type Frobble
        DynamicClasspath
        (can-read? [_] true))
      (is (readable-classpath? frobble)))))

