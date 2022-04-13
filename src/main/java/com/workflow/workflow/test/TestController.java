package com.workflow.workflow.test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@RestController
@RequestMapping("/tests")
public class TestController {
	@Autowired
	private TestRepository testRepository;

	@Operation(summary = "This method is used to retrive array with all test objects.")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "List of all test objects.", content = {
					@Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = Test.class)))
			})
	})
	@GetMapping("/")
	public Iterable<Test> all() {
		return testRepository.findAll();
	}

	@Operation(summary = "This method is used create test object.")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Newly created test object.", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = Test.class))
			})
	})
	@PostMapping("/")
	public Test add(@RequestBody TestRequest test) {
		return testRepository.save(new Test(test));
	}

	@Operation(summary = "This method is used to retrive test object.")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Test object with given id.", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = Test.class))
			}),
			@ApiResponse(responseCode = "404", description = "Object with given id not found.", content = {
					@Content()
			})
	})
	@GetMapping("/{id}")
	public Test one(@PathVariable Long id) {
		return testRepository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "entity not found"));
	}

	@Operation(summary = "This method is used to modify existing test objects.")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Modified test object with given id.", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = Test.class))
			}),
			@ApiResponse(responseCode = "404", description = "Object with given id not found.", content = {
					@Content()
			})
	})
	@PatchMapping("/{id}")
	public Test patch(@PathVariable Long id, @RequestBody TestRequest test) {
		Test employee = testRepository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "entity not found"));
		employee.patch(test);
		return testRepository.save(employee);
	}

	@Operation(summary = "This method is used to create or modify test objects.")
	@Parameter(name = "id", description = "Id of object to modify. When object with given id does not exists new one is created. Id of new object may not equal given one.", in = ParameterIn.PATH, required = true, schema = @Schema(implementation = Integer.class))
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Modified or created test object.", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = Test.class))
			})
	})
	@PutMapping("/{id}")
	public Test put(@PathVariable Long id, @RequestBody TestRequest test) {
		Test employee = new Test(test);
		employee.setId(id);
		return testRepository.save(employee);
	}

	@Operation(summary = "This method is used to delete test objects.")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Object with given id has been deleted."),
			@ApiResponse(responseCode = "404", description = "Object with given id not found.")
	})
	@DeleteMapping("/{id}")
	public void delete(@PathVariable Long id) {
		try {
			testRepository.deleteById(id);
		} catch (EmptyResultDataAccessException exception) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, exception.getMessage());
		}
	}
}
