package com.example

import org.scalatest.flatspec.AnyFlatSpec

class TestA extends AnyFlatSpec {
  it should "passA" in {
    assert(true)
  }

  it should "failA" in {
    assert(false)
  }
}
