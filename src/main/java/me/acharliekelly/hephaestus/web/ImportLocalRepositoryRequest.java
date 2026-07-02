package me.acharliekelly.hephaestus.web;

import jakarta.validation.constraints.NotBlank;

public record ImportLocalRepositoryRequest(@NotBlank String path) {
}

