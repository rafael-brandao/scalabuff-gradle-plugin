package multiproject.impl

import multiproject.api.Address

class Service

object Service {
  def main(args: Array[String]) {
    val address = Address(city = "Rio de Janeiro")

    println(address)
  }
}
