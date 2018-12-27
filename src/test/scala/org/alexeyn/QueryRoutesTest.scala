package org.alexeyn

import akka.http.scaladsl.model.{ContentTypes, StatusCodes}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import cats.instances.future._
import org.alexeyn.TestData._
import org.alexeyn.http.QueryRoutes
import org.scalatest.{Matchers, WordSpec}
import RequestsSupport._

import scala.concurrent.Future

class QueryRoutesTest extends WordSpec with Matchers with ScalatestRouteTest with SprayJsonCodes {
  private val mockDao = createMockDao
  private val service = new TripService[Future](mockDao)
  val routes: Route = QueryRoutes.routes(service)

  "QueryRoutes" should {
    "return all trips sorted by some parameter" in {
      val request = RequestsSupport.selectAllRequest("city")

      request ~> routes ~> check {
        commonChecks
        val all = entityAs[Trips].trips
        all.length should ===(3)
        all.map(_.city) should ===(Seq("berlin", "frankfurt", "munich"))
      }
    }

    "return all trips sorted by id by default" in {
      val request = selectAllRequest()

      request ~> routes ~> check {
        commonChecks
        val trips = entityAs[Trips].trips
        trips.length should ===(3)
        trips.map(_.id) should ===(Seq(1, 2, 3))
      }
    }
  }

  private def commonChecks = {
    status should ===(StatusCodes.OK)
    contentType should ===(ContentTypes.`application/json`)
  }

  private def createMockDao = {
    new Dao[Trip, Future] {
      override def createSchema(): Future[Unit] = Future.successful()
      override def insert(row: Trip): Future[Int] = Future.successful(1)
      override def selectAll(page: Int, pageSize: Int, sort: String): Future[Seq[Trip]] = {
        sort match {
          case "id" => Future.successful(mockData.sortBy(_.id))
          case "city" => Future.successful(mockData.sortBy(_.city))
        }
      }
      override def select(id: Int): Future[Option[Trip]] = Future.successful(mockData.lift(id))
      override def delete(id: Int): Future[Int] = ???
      override def update(id: Int, row: Trip): Future[Int] = ???
      override def sortingFields: Set[String] = Set("id", "city")
    }
  }
}