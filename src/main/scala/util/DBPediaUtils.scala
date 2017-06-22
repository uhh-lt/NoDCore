/*
 * Copyright 2015 Technische Universitaet Darmstadt
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  Contributors:
 *         - Uli Fahrer
 */

package util

import com.hp.hpl.jena.query.{QueryParseException, QueryExecution, QueryExecutionFactory, QueryFactory}
import com.hp.hpl.jena.sparql.engine.http.QueryExceptionHTTP

import scala.collection.mutable.ListBuffer


object DBPediaUtils {

  private val sparqlEndpoint = "http://dbpedia-live.openlinksw.com/sparql"
  private val rateLimitCode = 503
  private val rateLimitWaitingTime = 80000 // 1,2 Min
  private val queryTemplate =
    "prefix dbpedia-owl: <http://dbpedia.org/ontology/> "+
    "prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> "+
      "select ?thumbnail "+
      "where { <http://dbpedia.org/resource/%1$s> dbpedia-owl:wikiPageRedirects*/dbpedia-owl:thumbnail ?thumbnail . { <http://dbpedia.org/resource/%1$s> dbpedia-owl:wikiPageRedirects*/rdf:type dbpedia-owl:Person } UNION { <http://dbpedia.org/resource/%1$s> dbpedia-owl:wikiPageRedirects*/rdf:type dbpedia-owl:Organisation } . } limit 1"


  private val disambiguationMap: Map[String, Option[String]] = Map(
    ("Apple" -> Some("http://upload.wikimedia.org/wikipedia/commons/f/fa/Apple_logo_black.svg")),
    ("CDU" -> Some("http://upload.wikimedia.org/wikipedia/commons/thumb/6/6e/Cdu-logo.svg/300px-Cdu-logo.svg.png")),
    ("FDP" -> Some("http://upload.wikimedia.org/wikipedia/commons/thumb/6/6e/Freie_Demokratische_Partei%2C_Deutschland_%28logo_-_2005%29.svg/300px-Freie_Demokratische_Partei%2C_Deutschland_%28logo_-_2005%29.svg.png")),
    ("ÖVP" -> Some("http://upload.wikimedia.org/wikipedia/de/thumb/7/7a/OEVP_Logo.svg/150px-OEVP_Logo.svg.png")),
    ("FC Bayern" -> Some("http://upload.wikimedia.org/wikipedia/commons/thumb/c/c5/Logo_FC_Bayern_M%C3%BCnchen.svg/300px-Logo_FC_Bayern_M%C3%BCnchen.svg.png")),
    ("Guardiola" -> Some("http://upload.wikimedia.org/wikipedia/commons/thumb/e/e5/Guardiola_2010.jpg/300px-Guardiola_2010.jpg")),
    ("CSU" -> Some("http://upload.wikimedia.org/wikipedia/commons/thumb/c/ce/Bundesarchiv_B_145_Bild-F049272-0002%2C_M%C3%BCnchen%2C_CSU-Bundestagswahlkampf%2C_Strau%C3%9F.jpg/300px-Bundesarchiv_B_145_Bild-F049272-0002%2C_M%C3%BCnchen%2C_CSU-Bundestagswahlkampf%2C_Strau%C3%9F.jpg")),
    ("Grüne" -> Some("http://upload.wikimedia.org/wikipedia/commons/thumb/4/4b/B%C3%BCndnis_90_-_Die_Gr%C3%BCnen_Logo.svg/100px-B%C3%BCndnis_90_-_Die_Gr%C3%BCnen_Logo.svg.png")),
    ("Grünen" -> Some("http://upload.wikimedia.org/wikipedia/commons/thumb/4/4b/B%C3%BCndnis_90_-_Die_Gr%C3%BCnen_Logo.svg/100px-B%C3%BCndnis_90_-_Die_Gr%C3%BCnen_Logo.svg.png")),
    ("Europäische Union" -> Some("http://upload.wikimedia.org/wikipedia/commons/thumb/b/b7/Flag_of_Europe.svg/100px-Flag_of_Europe.svg.png")),
    ("Twitter" -> Some("http://upload.wikimedia.org/wikipedia/en/thumb/9/9f/Twitter_bird_logo_2012.svg/100px-Twitter_bird_logo_2012.svg.png")),
    ("Daimler" -> Some("http://upload.wikimedia.org/wikipedia/en/thumb/1/1b/Daimler_logo.jpg/300px-Daimler_logo.jpg")),
    ("Mercedes" -> Some("http://upload.wikimedia.org/wikipedia/en/thumb/b/bb/Mercedes_benz_silverlogo.png/100px-Mercedes_benz_silverlogo.png")),
    ("Red Bull" -> Some("http://upload.wikimedia.org/wikipedia/en/thumb/f/f5/RedBullEnergyDrink.svg/100px-RedBullEnergyDrink.svg.png")),
    ("BND" -> Some("http://upload.wikimedia.org/wikipedia/commons/thumb/a/a9/BND_Logo_neu.svg/300px-BND_Logo_neu.svg.png")),
    ("Schalke 04" -> Some("http://upload.wikimedia.org/wikipedia/commons/thumb/4/43/Schalke_04.svg/100px-Schalke_04.svg.png")),
    ("Schalke" -> Some("http://upload.wikimedia.org/wikipedia/commons/thumb/4/43/Schalke_04.svg/100px-Schalke_04.svg.png")),
    ("FC Schalke" -> Some("http://upload.wikimedia.org/wikipedia/commons/thumb/4/43/Schalke_04.svg/100px-Schalke_04.svg.png")),
    ("FC Schalke 04" -> Some("http://upload.wikimedia.org/wikipedia/commons/thumb/4/43/Schalke_04.svg/100px-Schalke_04.svg.png")),
    ("HSV" -> Some("http://upload.wikimedia.org/wikipedia/commons/thumb/6/66/HSV-Logo.svg/300px-HSV-Logo.svg.png")),
    ("Hertha BSC" -> Some("http://upload.wikimedia.org/wikipedia/en/thumb/5/5e/Hertha_Berlin_SC.png/100px-Hertha_Berlin_SC.png")),
    ("RB Leipzig" -> Some("http://upload.wikimedia.org/wikipedia/en/thumb/4/49/RB_Leipzig_2014_logo.svg.png/100px-RB_Leipzig_2014_logo.svg.png")),
    ("Real Madrid" -> Some("http://upload.wikimedia.org/wikipedia/en/thumb/5/56/Real_Madrid_CF.svg/71px-Real_Madrid_CF.svg.png")),
    ("FC Barcelona" -> Some("http://upload.wikimedia.org/wikipedia/en/thumb/4/47/FC_Barcelona_%28crest%29.svg/99px-FC_Barcelona_%28crest%29.svg.png")),
    ("Vfl Wolfsburg" -> Some("http://upload.wikimedia.org/wikipedia/en/thumb/8/88/Vfl_Wolfsburg.svg/100px-Vfl_Wolfsburg.svg.png")),
    ("VfL Wolfsburg" -> Some("http://upload.wikimedia.org/wikipedia/en/thumb/8/88/Vfl_Wolfsburg.svg/100px-Vfl_Wolfsburg.svg.png")),
    ("Usain Bolt" -> Some("http://upload.wikimedia.org/wikipedia/commons/thumb/8/8d/Usain_Bolt_by_Augustas_Didzgalvis_%28cropped%29.jpg/74px-Usain_Bolt_by_Augustas_Didzgalvis_%28cropped%29.jpg")),
    ("FIA" -> Some("http://upload.wikimedia.org/wikipedia/en/thumb/9/96/FIA_logo.svg/100px-FIA_logo.svg.png")),
    ("AfD" -> Some("http://upload.wikimedia.org/wikipedia/commons/thumb/b/b8/Alternative-fuer-Deutschland-Logo-2013.svg/300px-Alternative-fuer-Deutschland-Logo-2013.svg.png")),
    ("DFB" -> Some("http://upload.wikimedia.org/wikipedia/en/thumb/b/bd/DFBTriangles.svg/100px-DFBTriangles.svg.png")),
    ("Verdi" -> Some("http://upload.wikimedia.org/wikipedia/commons/thumb/d/d5/Syndicalism.svg/28px-Syndicalism.svg.png")),
    ("GDL" -> Some("http://upload.wikimedia.org/wikipedia/commons/b/ba/Logo_GDL.svg")),
    ("NHL" -> Some("http://upload.wikimedia.org/wikipedia/en/thumb/3/3a/05_NHL_Shield.svg/88px-05_NHL_Shield.svg.png")),
    ("Sky" -> Some("http://upload.wikimedia.org/wikipedia/en/thumb/f/fe/Sky_logo.png/100px-Sky_logo.png")),
    ("Apple Watch" -> Some("http://upload.wikimedia.org/wikipedia/commons/thumb/c/c0/White_AppleWatch_with_Screen.png/65px-White_AppleWatch_with_Screen.png")),
    ("EU-Kommission" -> Some("http://upload.wikimedia.org/wikipedia/en/thumb/8/84/European_Commission.svg/100px-European_Commission.svg.png")),
    ("EZB" -> Some("http://upload.wikimedia.org/wikipedia/de/2/26/Europ%C3%A4ische-Zentralbank-Logo.svg")),
    ("Europäische Zentralbank" -> Some("http://upload.wikimedia.org/wikipedia/de/2/26/Europ%C3%A4ische-Zentralbank-Logo.svg")),
    ("IWF" -> Some("http://upload.wikimedia.org/wikipedia/en/thumb/7/7e/International_Monetary_Fund_logo.svg/98px-International_Monetary_Fund_logo.svg.png")),
    ("Varoufakis" -> Some("http://upload.wikimedia.org/wikipedia/commons/thumb/4/40/Yanis-Varoufakis-Berlin-2015-02-05.jpg/67px-Yanis-Varoufakis-Berlin-2015-02-05.jpg")),
    ("Yanis Varoufakis" -> Some("http://upload.wikimedia.org/wikipedia/commons/thumb/4/40/Yanis-Varoufakis-Berlin-2015-02-05.jpg/67px-Yanis-Varoufakis-Berlin-2015-02-05.jpg"))
  )

  private def imageLinkForName(name: String): Option[String] = {

    var linkOpt: Option[String] = None
    val convertedName = convertToDBPediaName(name)

    val stringQuery = queryTemplate.format(convertedName)
    var qexec: Option[QueryExecution] = None

    try {

      val sparqlQuery = QueryFactory.create(stringQuery)
      qexec = Some(QueryExecutionFactory.sparqlService(sparqlEndpoint, sparqlQuery))

      val results = qexec.get.execSelect()
      //Ensure rate limit is never reached!
      Thread sleep 500
      if (results.hasNext()) {

        linkOpt = Some(results.next().get("thumbnail").toString)
      }
    }
    catch {
      case e: QueryExceptionHTTP =>
        println("Fetch image for %s failed!".format(name))

        if(e.getResponseCode == rateLimitCode) {
          //TODO: Rate limit here will not try name again
          println(s"Rate limit reached! Will sleep for ${rateLimitWaitingTime} ms.")
          Thread sleep rateLimitWaitingTime
        }
      case e: QueryParseException =>
        println("Query creation for entity %s failed!".format(name))
    }
    finally {
      qexec.get.close()
    }

    linkOpt
  }


  def imageLinksForNames(names: List[(Long, String)]): List[Seq[Any]]  = {

    val result = ListBuffer[Seq[Any]]()

    for((id, name) <- names) {

      val linkOpt: Option[String] = disambiguationMap.getOrElse(name, imageLinkForName(name))
      if(linkOpt.isDefined) {
        result += Seq(id, linkOpt.get)
      }
    }
    result.toList
  }

  //TODO: consider other  Maryam d`Abo
  def convertToDBPediaName(name: String): String = name.replace("\"", "").replace("`", "\'").split(" ").mkString("_")
}
