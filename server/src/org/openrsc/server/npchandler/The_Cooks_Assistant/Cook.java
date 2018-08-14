/**
* Generated By NPCScript :: A scripting engine created for openrsc by Zilent
*/
package org.openrsc.server.npchandler.The_Cooks_Assistant;

import org.openrsc.server.Config;
import org.openrsc.server.event.SingleEvent;
import org.openrsc.server.logging.Logger;
import org.openrsc.server.logging.model.eventLog;
import org.openrsc.server.model.*;
import org.openrsc.server.event.DelayedQuestChat;
import org.openrsc.server.npchandler.NpcHandler;
import org.openrsc.server.util.DataConversions;
public class Cook implements NpcHandler {
	
	public void handleNpc(final Npc npc, final Player owner) throws Exception {
		npc.blockedBy(owner);
		owner.setBusy(true);
		Quest q = owner.getQuest(Quests.COOKS_ASSISTANT);
		if(q != null) {
			if(q.finished()) { //Quest Finished
				final String[] messages7 = {"Hello friend, how is the adventuring going?"};
				World.getDelayedEventHandler().add(new DelayedQuestChat(npc, owner, messages7, true) {
					public void finished() {
						World.getDelayedEventHandler().add(new SingleEvent(owner, 1500) {
							public void action() {
								final String[] options2 = {"I am getting strong and mighty", "I keep on dying", "Nice hat", "Can I use your range?"};
								owner.setBusy(false);
								owner.sendMenu(options2);
								owner.setMenuHandler(new MenuHandler(options2) {
									public void handleReply(final int option, final String reply) {
										owner.setBusy(true);
										for(Player informee : owner.getViewArea().getPlayersInView()) {
											informee.informOfChatMessage(new ChatMessage(owner, reply, npc));
										}
										switch(option) {
											case 0:
												final String[] messages8 = {"Glad to hear it"};
												World.getDelayedEventHandler().add(new DelayedQuestChat(npc, owner, messages8) {
													public void finished() {
														owner.setBusy(false);
														npc.unblock();
													}
												});
												break;
											case 1:
												final String[] messages9 = {"Ah well at least you keep coming back to life!"};
												World.getDelayedEventHandler().add(new DelayedQuestChat(npc, owner, messages9) {
													public void finished() {
														owner.setBusy(false);
														npc.unblock();
													}
												});
												break;
											case 2:
												final String[] messages10 = {"Err thank you -it's a pretty ordinary cooks hat really"};
												World.getDelayedEventHandler().add(new DelayedQuestChat(npc, owner, messages10) {
													public void finished() {
														owner.setBusy(false);
														npc.unblock();
													}
												});
												break;
											case 3:
												final String[] messages11 = {"Go ahead", "It's a very good range", "It's easier to use than most other ranges"};
												World.getDelayedEventHandler().add(new DelayedQuestChat(npc, owner, messages11) {
													public void finished() {
														owner.setBusy(false);
														npc.unblock();
													}
												});
												break;
										}
									}
								});
							}
						});
					}
				});
			} else if(q.getStage() == 0) { // Quest Started
				World.getDelayedEventHandler().add(new DelayedQuestChat(npc, owner, new String[] {"How are you getting on with finding the ingredients?"}, true) {
					public void finished() {
						if(owner.getInventory().contains(19)) {
							if(owner.getInventory().contains(22)) {
								if(owner.getInventory().contains(136)) {
									World.getDelayedEventHandler().add(new DelayedQuestChat(owner, npc, new String[] {"I now have everything you need for your cake", "Milk, flour, and an egg!"}) {
										public void finished() {
											World.getDelayedEventHandler().add(new DelayedQuestChat(npc, owner, new String[] {"I am saved thankyou!"}) {
												public void finished() {
													World.getDelayedEventHandler().add(new SingleEvent(owner,1000) {
														public void action() {
															owner.sendMessage("You give some milk, an egg, and some flour to the cook");
															owner.getInventory().remove(new InvItem(19, 1));
															owner.getInventory().remove(new InvItem(22, 1));
															owner.getInventory().remove(new InvItem(136, 1));
															owner.sendInventory();
															World.getDelayedEventHandler().add(new SingleEvent(owner,1000) {
																public void action() {
																	owner.finishQuest(Quests.COOKS_ASSISTANT);
																	owner.sendMessage("Well done. You have completed the cook's assistant quest");
																	owner.sendMessage("@gre@You just advanced 1 quest point!");
																	owner.incQuestExp(Skills.COOKING, 180);
																	owner.sendStat(7);
																	owner.setBusy(false);
																	npc.unblock();
																	Logger.log(new eventLog(owner.getUsernameHash(), owner.getAccount(), owner.getIP(), DataConversions.getTimeStamp(), "<strong>" + owner.getUsername() + "</strong>" + " has completed the <span class=\"recent_quest\">Cook's Assistant</span> quest!"));
																}
															});
														}
													});
												}
											});
										}
									});
								} else {
								World.getDelayedEventHandler().add(new DelayedQuestChat(owner, npc, new String[] {"I have found some of the things you asked for:", "I have some milk", "I have an egg"}) {
									public void finished() {
										World.getDelayedEventHandler().add(new DelayedQuestChat(npc, owner, new String[] {"Great, but can you get the other ingredients as well?", "You still need to find", "Some flour"}) {
											public void finished() {
												World.getDelayedEventHandler().add(new DelayedQuestChat(owner, npc, new String[] {"Ok I'll try to find that for you"}) {
													public void finished() {
														owner.setBusy(false);
														npc.unblock();
													}
												});
											}
										});
									}
								});
								}
							} else if(owner.getInventory().contains(136)) {
								World.getDelayedEventHandler().add(new DelayedQuestChat(owner, npc, new String[] {"I have found some of the things you asked for:", "I have some flour", "I have an egg"}) {
									public void finished() {
										World.getDelayedEventHandler().add(new DelayedQuestChat(npc, owner, new String[] {"Great, but can you get the other ingredients as well?", "You still need to find", "Some milk"}) {
											public void finished() {
												World.getDelayedEventHandler().add(new DelayedQuestChat(owner, npc, new String[] {"Ok I'll try to find that for you"}) {
													public void finished() {
														owner.setBusy(false);
														npc.unblock();
													}
												});
											}
										});
									}
								});
							} else {
								World.getDelayedEventHandler().add(new DelayedQuestChat(owner, npc, new String[] {"I have found some of the things you asked for:", "I have an egg"}) {
									public void finished() {
										World.getDelayedEventHandler().add(new DelayedQuestChat(npc, owner, new String[] {"Great, but can you get the other ingredients as well?", "You still need to find", "Some milk", "Some flour"}) {
											public void finished() {
												World.getDelayedEventHandler().add(new DelayedQuestChat(owner, npc, new String[] {"Ok I'll try to find that for you"}) {
													public void finished() {
														owner.setBusy(false);
														npc.unblock();
													}
												});
											}
										});
									}
								});
							}
						} else if(owner.getInventory().contains(22)) {
							if(owner.getInventory().contains(136)) {
								World.getDelayedEventHandler().add(new DelayedQuestChat(owner, npc, new String[] {"I have found some of the things you asked for:", "I have some milk", "I have some flour"}) {
									public void finished() {
										World.getDelayedEventHandler().add(new DelayedQuestChat(npc, owner, new String[] {"Great, but can you get the other ingredients as well?", "You still need to find", "An egg"}) {
											public void finished() {
												World.getDelayedEventHandler().add(new DelayedQuestChat(owner, npc, new String[] {"Ok I'll try to find that for you"}) {
													public void finished() {
														owner.setBusy(false);
														npc.unblock();
													}
												});
											}
										});
									}
								});
							} else {
								World.getDelayedEventHandler().add(new DelayedQuestChat(owner, npc, new String[] {"I have found some of the things you asked for:", "I have some milk"}) {
									public void finished() {
										World.getDelayedEventHandler().add(new DelayedQuestChat(npc, owner, new String[] {"Great, but can you get the other ingredients as well?", "You still need to find", "Some flour", "An egg"}) {
											public void finished() {
												World.getDelayedEventHandler().add(new DelayedQuestChat(owner, npc, new String[] {"Ok I'll try to find that for you"}) {
													public void finished() {
														owner.setBusy(false);
														npc.unblock();
													}
												});
											}
										});
									}
								});
							}
						} else if(owner.getInventory().contains(136)) {
							World.getDelayedEventHandler().add(new DelayedQuestChat(owner, npc, new String[] {"I have found some of the things you asked for:", "I have some flour"}) {
								public void finished() {
									World.getDelayedEventHandler().add(new DelayedQuestChat(npc, owner, new String[] {"Great, but can you get the other ingredients as well?", "You still need to find", "Some milk", "An egg"}) {
										public void finished() {
											World.getDelayedEventHandler().add(new DelayedQuestChat(owner, npc, new String[] {"Ok I'll try to find that for you"}) {
												public void finished() {
													owner.setBusy(false);
													npc.unblock();
												}
											});
										}
									});
								}
							});
						} else {
							World.getDelayedEventHandler().add(new DelayedQuestChat(owner, npc, new String[] {"I'm afraid I don't have any yet!"}) {
								public void finished() {
									World.getDelayedEventHandler().add(new DelayedQuestChat(npc, owner, new String[] {"Oh dear oh dear!", "I need flour, eggs, and milk", "Without them I am doomed!"}) {
										public void finished() {
											owner.setBusy(false);
											npc.unblock();
										}
									});
								}
							});
						}
					}
				});
			}
		} else { //Quest Not Started
			final String[] messages0 = {"What am I to do?"};
			World.getDelayedEventHandler().add(new DelayedQuestChat(npc, owner, messages0, true) {
				public void finished() {
					World.getDelayedEventHandler().add(new SingleEvent(owner, 1500) {
						public void action() {
							final String[] options0 = {"What's wrong?", "Well you could give me all your money", "You don't look very happy", "Nice hat"};
							owner.setBusy(false);
							owner.sendMenu(options0);
							owner.setMenuHandler(new MenuHandler(options0) {
								public void handleReply(final int option, final String reply) {
									owner.setBusy(true);
									for(Player informee : owner.getViewArea().getPlayersInView()) {
										informee.informOfChatMessage(new ChatMessage(owner, reply, npc));
									}
									switch(option) {
										case 0:
											whatsWrong(npc, owner);
											break;
										case 1:
											final String[] messages1 = {"HaHa very funny"};
											World.getDelayedEventHandler().add(new DelayedQuestChat(npc, owner, messages1) {
												public void finished() {
													owner.setBusy(false);
													npc.unblock();
												}
											});
											break;
										case 2:
											final String[] messages2 = {"No, I'm not"};
											World.getDelayedEventHandler().add(new DelayedQuestChat(npc, owner, messages2) {
												public void finished() {
													whatsWrong(npc, owner);
												}
											});
											break;
										case 3:
											final String[] messages3 = {"Err thank you -it's a pretty ordinary cooks hat really"};
											World.getDelayedEventHandler().add(new DelayedQuestChat(npc, owner, messages3) {
												public void finished() {
													owner.setBusy(false);
													npc.unblock();
												}
											});
											break;
									}
								}
							});
						}
					});
				}
			});
		}
	}
	private void whatsWrong(final Npc npc, final Player owner) {
		final String[] messages4 = {"Ooh dear I'm in a terrible mess", "It's the duke's birthday today", "I'm meant to be making him a big cake for this evening", "Unfortunately, I've forgotten to buy some of the ingredients", "I'll never get them in time now", "I don't suppose you could help me?"};
		World.getDelayedEventHandler().add(new DelayedQuestChat(npc, owner, messages4) {
			public void finished() {
				World.getDelayedEventHandler().add(new SingleEvent(owner, 1500) {
					public void action() {
						final String[] options1 = {"Yes, I'll help you", "No, I don't feel like it. Maybe later"};
						owner.setBusy(false);
						owner.sendMenu(options1);
						owner.setMenuHandler(new MenuHandler(options1) {
							public void handleReply(final int option, final String reply) {
								owner.setBusy(true);
								for(Player informee : owner.getViewArea().getPlayersInView()) {
									informee.informOfChatMessage(new ChatMessage(owner, reply, npc));
								}
								switch(option) {
									case 0:
										final String[] messages5 = {"Oh thank you, thank you", "I need milk, eggs, and flour", "I'd be very grateful if you can get them to me"};
										World.getDelayedEventHandler().add(new DelayedQuestChat(npc, owner, messages5) {
											public void finished() {
												owner.addQuest(Quests.COOKS_ASSISTANT, 1);
												owner.setBusy(false);
												npc.unblock();
											}
										});
										break;
									case 1:
										final String[] messages6 = {"OK, suit yourself"};
										World.getDelayedEventHandler().add(new DelayedQuestChat(npc, owner, messages6) {
											public void finished() {
												owner.setBusy(false);
												npc.unblock();
											}
										});
										break;
								}
							}
						});
					}
				});
			}
		});
	}
}