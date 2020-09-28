package com.example

import org.scalatest.flatspec.AnyFlatSpec

class TestB extends AnyFlatSpec {
  it should "passB" in {
    assert(true)
  }

  it should "failB" in {
    assert(false)
  }
}
