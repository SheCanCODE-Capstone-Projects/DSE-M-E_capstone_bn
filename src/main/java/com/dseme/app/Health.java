package com.dseme.app;
import org.springframework.web.bind.annotation.*;

@RestController
public class Health {
 @GetMapping("/health")
 public String ok(){ return "DSE M&E backend running"; }
}
