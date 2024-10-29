package com.rockthejvm.reviewboard.services

import com.rockthejvm.reviewboard.domain.data.{Company, CompanyFilter}
import com.rockthejvm.reviewboard.http.requests.CreateCompanyRequest
import com.rockthejvm.reviewboard.repositories.CompanyRepository

import scala.collection.mutable
import zio.*

trait CompanyService {

  def create(req: CreateCompanyRequest): Task[Company]
  def getAll: Task[List[Company]]
  def getById(id: Long): Task[Option[Company]]
  def getBySlug(slug: String): Task[Option[Company]]
  def allFilters: Task[CompanyFilter]
  def search(filter: CompanyFilter):Task[List[Company]]
}

class CompanyServiceLive private (repo: CompanyRepository) extends CompanyService {
  override def create(req: CreateCompanyRequest): Task[Company] = repo.create(req.toCompany(-1L))
  override def getAll: Task[List[Company]]                      = repo.get
  override def getById(id: Long): Task[Option[Company]]         = repo.getById(id)
  override def getBySlug(slug: String): Task[Option[Company]]   = repo.getBySlug(slug)
  override def allFilters: Task[CompanyFilter]                  = repo.uniqueAttributes
  override def search(filter: CompanyFilter): Task[List[Company]] = repo.search(filter)
}

object CompanyServiceLive {

  val layer = ZLayer {
    for {
      repo <- ZIO.service[CompanyRepository]
    } yield new CompanyServiceLive(repo)
  }
}
