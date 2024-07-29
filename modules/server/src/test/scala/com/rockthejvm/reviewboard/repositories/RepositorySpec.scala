package com.rockthejvm.reviewboard.repositories

import org.postgresql.ds.PGSimpleDataSource
import org.testcontainers.containers.PostgreSQLContainer
import zio.{ZIO, ZLayer}

import javax.sql.DataSource

trait RepositorySpec {

  val initScript: String
  
  // Create docker test container
  def createContainer() = {
    val container: PostgreSQLContainer[Nothing] =
      PostgreSQLContainer("postgres").withInitScript(initScript)
    container.start()
    container
  }

  def closeContainer(container: PostgreSQLContainer[Nothing]) = {
    container.stop()
  }

  // Create data source
  def createDatasource(container: PostgreSQLContainer[Nothing]): DataSource = {

    val dataSource = new PGSimpleDataSource
    dataSource.setUrl(container.getJdbcUrl)
    dataSource.setUser(container.getUsername)
    dataSource.setPassword(container.getPassword)
    dataSource
  }

  // use the DataSource as ZLayer to build QUIll instance as a ZLayer
  val dataSourceLayer = ZLayer {
    for {
      container <- ZIO.acquireRelease(ZIO.attempt(createContainer()))(container =>
        ZIO.attempt(container.stop()).ignoreLogged
      )
      dataSource <- ZIO.attempt(createDatasource(container))
    } yield dataSource
  }

}
