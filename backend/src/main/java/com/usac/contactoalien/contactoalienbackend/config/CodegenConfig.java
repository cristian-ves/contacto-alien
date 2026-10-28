package com.usac.contactoalien.contactoalienbackend.config;

import com.usac.contactoalien.contactoalienbackend.codegen.TargetConfig;
import com.usac.contactoalien.contactoalienbackend.codegen.exec.interactive.RunLimits;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

// Registers the target toolchain and run-limit settings so they can be injected where programs are built and run.
@Configuration
@EnableConfigurationProperties({TargetConfig.class, RunLimits.class})
public class CodegenConfig {
}
