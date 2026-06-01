package io.spring.api;

import static io.restassured.module.mockmvc.RestAssuredMockMvc.given;
import static io.spring.TestHelper.articleDataFixture;
import static java.util.Arrays.asList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import io.restassured.filter.Filter;
import io.restassured.filter.FilterContext;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import io.restassured.response.Response;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.FilterableResponseSpecification;
import io.spring.JacksonCustomizations;
import io.spring.api.security.WebSecurityConfig;
import io.spring.application.ArticleQueryService;
import io.spring.application.Page;
import io.spring.application.article.ArticleCommandService;
import io.spring.application.data.ArticleDataList;
import io.spring.core.article.ArticleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.logging.Level;
import java.util.logging.Logger;

@WebMvcTest(ArticlesApi.class)
@Import({WebSecurityConfig.class, JacksonCustomizations.class})
public class ListArticleApiTest extends TestWithCurrentUser {
  @MockBean private ArticleRepository articleRepository;

  @MockBean private ArticleQueryService articleQueryService;

  @MockBean private ArticleCommandService articleCommandService;

  @Autowired private MockMvc mvc;

  private static final Logger logger = Logger.getLogger(ListArticleApiTest.class.getName());

  @Override
  @BeforeEach
  public void setUp() throws Exception {
    super.setUp();
    RestAssuredMockMvc.mockMvc(mvc);

    RestAssuredMockMvc.filters(new Filter() {
      @Override
      public Response filter(FilterableRequestSpecification requestSpec, FilterableResponseSpecification responseSpec, FilterContext ctx) {
        long start = System.currentTimeMillis();
        Response response = ctx.next(requestSpec, responseSpec);
        long elapsed = System.currentTimeMillis() - start;
        String timestamp = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ").format(new java.util.Date());
        logger.log(Level.INFO, "Request: timestamp={0}, method={1}, path={2}, status={3}, responseTimeMs={4}",
            new Object[]{timestamp, requestSpec.getMethod(), requestSpec.getDerivedPath(), response.getStatusCode(), elapsed});
        return response;
      }
    });
  }

  @Test
  public void should_get_default_article_list() throws Exception {
    ArticleDataList articleDataList =
        new ArticleDataList(
            asList(articleDataFixture("1", user), articleDataFixture("2", user)), 2);
    when(articleQueryService.findRecentArticles(
            eq(null), eq(null), eq(null), eq(new Page(0, 20)), eq(null)))
        .thenReturn(articleDataList);
    RestAssuredMockMvc.when().get("/articles").prettyPeek().then().statusCode(200);
  }

  @Test
  public void should_get_feeds_401_without_login() throws Exception {
    RestAssuredMockMvc.when().get("/articles/feed").prettyPeek().then().statusCode(401);
  }

  @Test
  public void should_get_feeds_success() throws Exception {
    ArticleDataList articleDataList =
        new ArticleDataList(
            asList(articleDataFixture("1", user), articleDataFixture("2", user)), 2);
    when(articleQueryService.findUserFeed(eq(user), eq(new Page(0, 20))))
        .thenReturn(articleDataList);

    given()
        .header("Authorization", "Token " + token)
        .when()
        .get("/articles/feed")
        .prettyPeek()
        .then()
        .statusCode(200);
  }
}
