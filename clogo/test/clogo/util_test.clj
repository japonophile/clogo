(ns clogo.util-test
  (:require [clojure.test :refer :all]
            [clogo.util :refer :all]))

(deftest formatstr-should-accept-string-argument
  (testing "formatstr should accept string argument"
    (is (= "This is a string argument" (formatstr "This is a {1} argument" "string")))))

(deftest formatstr-should-accept-integer-argument
  (testing "formatstr should accept integer argument"
    (is (= "This is 1 integer argument" (formatstr "This is {1} integer argument" 1)))))

(deftest formatstr-should-accept-float-argument
  (testing "formatstr should accept float argument"
    (is (= "This is 2.1234 float argument" (formatstr "This is {1} float argument" 2.1234)))))

(deftest formatstr-should-replace-multiple-arguments
  (testing "formatstr should replace multiple arguments"
    (is (= "These are mult1ple arguments" (formatstr "These {1} mult{2}ple arguments" "are" 1)))))

(deftest formatstr-should-respect-argument-order
  (testing "formatstr should respect argument order"
    (is (= "are The order in random arguments" (formatstr "{3} {1} {6} {4} {5} {2}" "The" "arguments" "are" "in" "random" "order")))))

(deftest vdiv-should-divide-vector-by-divider
  (testing "vdiv should divide a vector by a divider"
    (is (= [3 5/2] (vdiv [6 5] 2)))))
