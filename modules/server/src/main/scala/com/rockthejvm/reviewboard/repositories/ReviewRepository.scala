package com.rockthejvm.reviewboard.repositories

import com.rockthejvm.reviewboard.domain.data.Review
import io.getquill.SnakeCase
import io.getquill.jdbczio.Quill
import zio.*

trait ReviewRepository {

  def create(review: Review): Task[Review]
  def getById(id: Long): Task[Option[Review]]
  def getByCompanyId(id: Long): Task[List[Review]]
  def getByUserId(id: Long): Task[List[Review]]
  def update(id: Long, op: Review => Review): Task[Review]
  def delete(id: Long): Task[Review]
}

class ReviewRepositoryLive private (quill: Quill.Postgres[SnakeCase]) extends ReviewRepository {

  def create(review: Review): Task[Review] = ZIO.fail(new RuntimeException("Not implemented"))

  def getById(id: Long): Task[Option[Review]] = ZIO.fail(new RuntimeException("Not implemented"))

  def getByCompanyId(id: Long): Task[List[Review]] =
    ZIO.fail(new RuntimeException("Not implemented"))

  def getByUserId(id: Long): Task[List[Review]] = ZIO.fail(new RuntimeException("Not implemented"))

  def update(id: Long, op: Review => Review): Task[Review] =
    ZIO.fail(new RuntimeException("Not implemented"))

  def delete(id: Long): Task[Review] = ZIO.fail(new RuntimeException("Not implemented"))
}

object ReviewRepositoryLive {

  val layer = ZLayer {
    ZIO.service[Quill.Postgres[SnakeCase.type]].map(quill => ReviewRepositoryLive(quill))
  }
}
