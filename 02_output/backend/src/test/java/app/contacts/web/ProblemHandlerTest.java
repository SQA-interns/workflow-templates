package app.contacts.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import app.contacts.service.FieldProblem;
import app.contacts.service.ForbiddenException;
import app.contacts.service.NotFoundException;
import app.contacts.service.ValidationException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

class ProblemHandlerTest {

  private final MockMvc mvc =
      MockMvcBuilders.standaloneSetup(new Throwing())
          .setControllerAdvice(new ProblemHandler())
          .build();

  @Test
  void br02NotFoundIs404WithCodeNotFound() throws Exception {
    mvc.perform(get("/not-found"))
        .andExpect(status().isNotFound())
        .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.code").value("notFound"));
  }

  @Test
  void br03ForbiddenIs403WithCodeAuthForbidden() throws Exception {
    mvc.perform(get("/forbidden"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("auth.forbidden"));
  }

  @Test
  void sb01ValidationIs400WithAnErrorPerField() throws Exception {
    mvc.perform(get("/invalid"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("validation"))
        .andExpect(jsonPath("$.errors[0].field").value("firstName"))
        .andExpect(jsonPath("$.errors[0].code").value("field.tooLong"))
        .andExpect(jsonPath("$.errors[0].params.max").value(100));
  }

  @Test
  void sb01UnreadableBodyIs400Validation() throws Exception {
    mvc.perform(post("/echo").contentType(MediaType.APPLICATION_JSON).content("{not json"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("validation"));
  }

  @Test
  void sb07UnexpectedErrorIs500InternalWithoutDetails() throws Exception {
    mvc.perform(get("/boom"))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.code").value("internal"))
        .andExpect(content().string(not(containsString("secret detail"))));
  }

  @RestController
  static class Throwing {
    @GetMapping("/not-found")
    void notFound() {
      throw new NotFoundException();
    }

    @GetMapping("/forbidden")
    void forbidden() {
      throw new ForbiddenException();
    }

    @GetMapping("/invalid")
    void invalid() {
      throw new ValidationException(
          List.of(new FieldProblem("firstName", "field.tooLong", Map.of("max", 100))));
    }

    @PostMapping("/echo")
    String echo(@RequestBody Map<String, Object> body) {
      return body.toString();
    }

    @GetMapping("/boom")
    void boom() {
      throw new IllegalStateException("secret detail");
    }
  }
}
