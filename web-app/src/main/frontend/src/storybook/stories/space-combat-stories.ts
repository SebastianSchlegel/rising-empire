import SpaceCombat from '~/page/main-page/component/inspector/component/space-combat';
import Story from '~/storybook/stories/story';

export default class SpaceCombatStories {
	static showSpaceCombatPageInUnexploredSystem(story: Story) {
		story.setRenderData(`{
			"attackerRace": "MYXALOR",
			"attackerColor": "red",
			"defenderRace": "LUMERISKS",
			"defenderColor": "blue",
			"_actions": [
				{ "fields": [], "name": "continue" }
			]
		}`);

		story.showHtml(`<${SpaceCombat.NAME} class="inspector-child-story"></${SpaceCombat.NAME}>`);
	}

	static showSpaceCombatInExploredSystem(story: Story) {
		story.setRenderData(`{
				"systemName": {
					"name": "Sol"
				},
				"habitability": {
					"type": "OCEAN",
					"special": "ARTIFACTS",
					"maxPopulation": 250
				},
				"attackerRace": "MYXALOR",
				"attackerColor": "red",
				"defenderRace": "LUMERISKS",
				"defenderColor": "blue",
				"_actions": [
					{ "fields": [], "name": "continue" }
				]
			}`);

		story.showHtml(`<${SpaceCombat.NAME} class="inspector-child-story"></${SpaceCombat.NAME}>`);
	}
}
