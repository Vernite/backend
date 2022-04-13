package com.workflow.workflow.test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/tests")
public class TestController {
    @Autowired
	private TestRepository employeeRepository;

	@GetMapping("/")
	public Iterable<Test> all() {
		return employeeRepository.findAll();
	}

	@PostMapping("/")
	public Test add(@RequestBody TestRequest employee) {
		return employeeRepository.save(new Test(employee));
	}
        
    @GetMapping("/{id}")
	public Test one(@PathVariable Long id) {
		return employeeRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "entity not found"));
	}

	@PatchMapping("/{id}")
	public Test patch(@PathVariable Long id, @RequestBody TestRequest employeeRequest) {
		Test employee = employeeRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "entity not found"));
		employee.patch(employeeRequest);
		return employeeRepository.save(employee);
	}

	@PutMapping("/{id}")
	public Test put(@PathVariable Long id, @RequestBody TestRequest employeeRequest) {
		Test employee = new Test(employeeRequest);
		employee.setId(id);
		return employeeRepository.save(employee);
	}
}
