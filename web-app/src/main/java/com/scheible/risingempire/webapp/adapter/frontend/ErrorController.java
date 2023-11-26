package com.scheible.risingempire.webapp.adapter.frontend;

import com.blueconic.browscap.Capabilities;
import com.blueconic.browscap.UserAgentParser;
import com.scheible.risingempire.webapp.adapter.frontend.context.FrontendContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * @author sj
 */
@RestController
@RequestMapping(produces = APPLICATION_JSON_VALUE)
class ErrorController {

	private static final Logger logger = LoggerFactory.getLogger(ErrorController.class);

	private final UserAgentParser userAgentParser;

	ErrorController(UserAgentParser userAgentParser) {
		this.userAgentParser = userAgentParser;
	}

	@PostMapping(path = { "/game-browser/errors", "/game/games/{gameId}/{player}/errors" },
			consumes = APPLICATION_JSON_VALUE)
	void receiveError(@RequestHeader(value = "User-Agent") String userAgent,
			@ModelAttribute final FrontendContext context, @RequestBody FrontendErrorBodyDto body) {
		Capabilities userAgentCapabilities = this.userAgentParser.parse(userAgent);

		String browser = userAgentCapabilities.getDeviceType() + " " + userAgentCapabilities.getBrowser() + " "
				+ userAgentCapabilities.getBrowserMajorVersion() + " for " + userAgentCapabilities.getPlatform() + " "
				+ userAgentCapabilities.getPlatformVersion();
		String errorLocation = body.fileName + ":" + body.lineNumber + ":" + body.columnNumber;
		if (context.getPlayer() != null && context.getGameId() != null) {

			logger.error("Frontend error '{}' for {} in game '{}' with browser '{}' at {}", body.message,
					context.getPlayer(), context.getGameId(), browser, errorLocation);
		}
		else {
			logger.error("Frontend error '{}' with browser '{}' at {}", body.message, browser, errorLocation);
		}
	}

	static class FrontendErrorBodyDto {

		int columnNumber;

		String fileName;

		int lineNumber;

		String message;

		// String stack;

	}

}
