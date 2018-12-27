package org.alexeyn

import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import cats.instances.future._
import org.alexeyn.TestData._
import org.alexeyn.http.CommandRoutes
import org.scalatest.{Matchers, WordSpec}

import scala.concurrent.Future

class CommandRoutesTest extends WordSpec with Matchers with ScalatestRouteTest with SprayJsonCodes {

  private val mockDao = createMockDao
  private val service = new TripService[Future](mockDao)

  val routes: Route = CommandRoutes.routes(service)

  "CommandRoutes" should {
    "insert new carAd and return its id" in {
      val request = RequestsSupport.insertRequest(berlin)

      request ~> routes ~> check {
        commonChecks
        val count = entityAs[CommandResult].count
        count should ===(1)
      }
    }

    "update existing carAd and return count" in {
      val request = RequestsSupport.insertRequest(berlin)

      request ~> routes ~> check {
        commonChecks
        val count = entityAs[CommandResult].count
        count should ===(1)
      }
    }

    "delete existing carAd and return count" in {
      val request = RequestsSupport.deleteRequest(adId)

      request ~> routes ~> check {
        commonChecks
        val count = entityAs[CommandResult].count
        count should ===(1)
      }
    }

    "reject new car creation when mileage is set for a new car" in {
      val request = RequestsSupport.insertRequest(berlin.copy(completed = true, distance = None))

      request ~> routes ~> check {
        status should ===(StatusCodes.PreconditionFailed)
      }
    }

    "reject new car creation when firstRegistration is set for a new car" in {
      val request = RequestsSupport.insertRequest(berlin.copy(completed = true, distance = None))

      request ~> routes ~> check {
        status should ===(StatusCodes.PreconditionFailed)
      }
    }

    "reject existing car modification when mileage is set for a new car" in {
      val request = RequestsSupport.updateRequest(berlin.copy(completed = true, distance = None), adId)

      request ~> routes ~> check {
        status should ===(StatusCodes.PreconditionFailed)
      }
    }

    "reject existing car modification when firstRegistration is set for a new car" in {
      val request = RequestsSupport.updateRequest(berlin.copy(completed = true, distance = None), adId)

      request ~> routes ~> check {
        status should ===(StatusCodes.PreconditionFailed)
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
      override def insert(row: Trip): Future[Int] = Future.successful(adId)
      override def selectAll(page: Int, pageSize: Int, sort: String): Future[Seq[Trip]] = ???
      override def select(id: Int): Future[Option[Trip]] = ???
      override def delete(id: Int): Future[Int] = Future.successful(adId)
      override def update(id: Int, row: Trip): Future[Int] = Future.successful(adId)
      override def sortingFields: Set[String] = ???
    }
  }
}