package com.litovskiy.config.properties;


import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.validator.constraints.Range;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@RefreshScope
@ConfigurationProperties("game.children")
public class ChildrenProperties {

    @Min(1)
    @NotNull
    private Integer maxHealth;

    @Min(1)
    @NotNull
    private Integer successHealthDelta;

    @Min(1)
    @NotNull
    private Integer failHealthDelta;

    @NotNull
    @Range(min = 0, max = 1)
    private Double singleSuccessChance;

    @NotNull
    @Range(min = 0, max = 1)
    private Double bothSuccessChance;

    @NotNull
    @Range(min = 0, max = 1)
    private Double sameActionSuccessChance;

    @NotNull
    @Range(min = 0, max = 1)
    private Double growthParentBuff;

    @NotNull
    @Range(min = -1, max = 0)
    private Double growthParentDebuff;
}
