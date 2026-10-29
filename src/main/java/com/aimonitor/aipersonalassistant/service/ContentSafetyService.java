
package com.aimonitor.aipersonalassistant.service;

import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

@Service
public class ContentSafetyService {

    private static final Pattern EXPLICIT_MEDIA_REQUEST =
            Pattern.compile(
                    "\\b(?:write|create|generate|produce|describe|show(?: me)?|"
                    + "find(?: me)?|send(?: me)?|give me|tell me|make|"
                    + "i want|want)\\b"
                    + ".{0,100}\\b(?:porn(?:ography)?|hentai|xxx|"
                    + "erotic (?:story|scene|roleplay)|"
                    + "explicit sexual (?:story|scene|content)|"
                    + "sex scene|sexual roleplay|"
                    + "nude (?:photo|image|picture)|"
                    + "naked (?:photo|image|picture)|sexting)\\b",
                    Pattern.CASE_INSENSITIVE | Pattern.DOTALL
            );

    private static final Pattern EXPLICIT_INSTRUCTIONS =
            Pattern.compile(
                    "\\b(?:how to|teach me to|give me (?:steps|instructions) to|"
                    + "steps to|instructions for)\\b"
                    + ".{0,100}\\b(?:masturbat\\w*|perform oral sex|"
                    + "give (?:someone )?(?:a )?(?:blowjob|handjob)|"
                    + "anal sex|explicit sexual acts|"
                    + "sexual penetration)\\b",
                    Pattern.CASE_INSENSITIVE | Pattern.DOTALL
            );

    private static final Pattern EXPLICIT_ROLEPLAY_REQUEST =
            Pattern.compile(
                    "\\b(?:let'?s|start|begin|do|write|create|"
                    + "act out|pretend|roleplay as)\\b"
                    + ".{0,100}\\b(?:sexual|erotic|explicit)\\b"
                    + ".{0,50}\\b(?:roleplay|role-play|scene|fantasy)\\b",
                    Pattern.CASE_INSENSITIVE | Pattern.DOTALL
            );

    public boolean isDisallowed(String message) {

        if (message == null || message.isBlank()) {
            return false;
        }

        String normalized = message.trim();

        return EXPLICIT_MEDIA_REQUEST.matcher(normalized).find()
                || EXPLICIT_INSTRUCTIONS.matcher(normalized).find()
                || EXPLICIT_ROLEPLAY_REQUEST.matcher(normalized).find();
    }
}
