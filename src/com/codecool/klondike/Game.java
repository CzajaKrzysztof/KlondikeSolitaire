package com.codecool.klondike;

import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.BackgroundPosition;
import javafx.scene.layout.BackgroundRepeat;
import javafx.scene.layout.BackgroundSize;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.control.Button;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

public class Game extends Pane {

    private List<Card> deck = new ArrayList<>();
    Stage attachedStage;
    Stage win;
    Button button;

    private Pile stockPile;
    private Pile discardPile;
    private List<Pile> foundationPiles = FXCollections.observableArrayList();
    private List<Pile> tableauPiles = FXCollections.observableArrayList();

    private double dragStartX, dragStartY;
    private List<Card> draggedCards = FXCollections.observableArrayList();

    private static double STOCK_GAP = 1;
    private static double FOUNDATION_GAP = 0;
    private static double TABLEAU_GAP = 30;

    private EventHandler<MouseEvent> onMouseClickedHandler = e -> {
        Card card = (Card) e.getSource();
        if (card.getContainingPile().getPileType() == Pile.PileType.STOCK) {
            card.moveToPile(discardPile);
            card.flip();
            card.setMouseTransparent(false);
            System.out.println("Placed " + card + " to the waste.");
        } else if (card.getContainingPile().getPileType() == Pile.PileType.TABLEAU
                && card.getContainingPile().getTopCard() == card && card.isFaceDown()) {
            card.flip();
            card.setMouseTransparent(false);
        }
    };

    private EventHandler<MouseEvent> stockReverseCardsHandler = e -> {
        refillStockFromDiscard();
    };

    private EventHandler<MouseEvent> onMousePressedHandler = e -> {
        dragStartX = e.getSceneX();
        dragStartY = e.getSceneY();
    };

    private EventHandler<MouseEvent> onMouseDraggedHandler = e -> {
        Card card = (Card) e.getSource();
        Pile activePile = card.getContainingPile();
        if (activePile.getPileType() == Pile.PileType.STOCK)
            return;
        double offsetX = e.getSceneX() - dragStartX;
        double offsetY = e.getSceneY() - dragStartY;

        draggedCards.clear();
        int indexOfCard = activePile.getCards().indexOf(card);
        for (int i = indexOfCard; i < activePile.getCards().size(); i++) {
            Card c = activePile.getCards().get(i);
            if (!c.isFaceDown() && activePile.getPileType() == Pile.PileType.TABLEAU) {
                cardMovement(c, offsetX, offsetY);
            } else if (activePile.getPileType() == Pile.PileType.DISCARD) {
                draggedCards.clear();
                cardMovement(c, offsetX, offsetY);

            } else if (activePile.getPileType() == Pile.PileType.FOUNDATION) {
                draggedCards.clear();
                cardMovement(c, offsetX, offsetY);
            }
        }

    };

    private void cardMovement(Card card, double offsetX, double offsetY) {
        draggedCards.add(card);

        card.getDropShadow().setRadius(20);
        card.getDropShadow().setOffsetX(10);
        card.getDropShadow().setOffsetY(10);

        card.toFront();
        card.setTranslateX(offsetX);
        card.setTranslateY(offsetY);
    }

    private EventHandler<MouseEvent> onMouseReleasedHandler = e -> {
        if (draggedCards.isEmpty())
            return;
        Card card = (Card) e.getSource();

        List<Pile> piles = new ArrayList<>();
        piles.addAll(tableauPiles);
        piles.addAll(foundationPiles);

        Pile pile = getValidIntersectingPile(card, piles);

        if (pile != null) {
            handleValidMove(card, pile);
        } else {
            draggedCards.forEach(MouseUtil::slideBack);
            draggedCards.clear();
        }
        if (isGameWon()) {
            System.out.println("DUUPA");
            gameWon(this.attachedStage);
        }
    };

    private boolean isGameWon() {
        int completedPiles = 0;
        int pileOneToComplete = 0;
        for (Pile pile : foundationPiles) {
            if (pile.numOfCards() == 13) {
                completedPiles++;
            } else if (pile.numOfCards() == 12) {
                pileOneToComplete++;

            }
        }
        return (completedPiles == 3 && pileOneToComplete == 1);
    }

    public Game(Stage primary) {
        deck = Card.createNewDeck();
        initPiles();
        dealCards();
        this.attachedStage = primary;
    }

    private void addMouseEventHandlers(Card card) {
        card.setOnMousePressed(onMousePressedHandler);
        card.setOnMouseDragged(onMouseDraggedHandler);
        card.setOnMouseReleased(onMouseReleasedHandler);
        card.setOnMouseClicked(onMouseClickedHandler);
    }

    private void refillStockFromDiscard() {
        Iterator<Card> discardIterator = discardPile.getCards().iterator();
        Collections.reverse(discardPile.getCards());
        while (discardIterator.hasNext()) {
            Card card = discardIterator.next();
            card.flip();
            stockPile.addCard(card);
            ;
        }
        discardPile.clear();
        System.out.println("Stock refilled from discard pile.");
    }

    private boolean isMoveValid(Card card, Pile destPile) {
        Card topCardInPile = destPile.getTopCard();

        if (tableauPiles.contains(destPile)) {
            return isMoveValidInTableauPile(card, topCardInPile, destPile);
        } else if (foundationPiles.contains(destPile)) {
            return isMoveValidInFoundationPile(card, topCardInPile, destPile);
        }
        return false;
    }

    private boolean isMoveValidInTableauPile(Card card, Card topCardInPile, Pile destPile) {
        if (card.getRank().getValue() == (Ranks.KING.getValue()) && destPile.numOfCards() == 0) {
            return true;
        } else if ((card.getRank().getValue() != (Ranks.KING.getValue())) && destPile.numOfCards() == 0) {
            return false;
        }
        return (Card.isOppositeColor(card, topCardInPile) && Card.isLower(card, topCardInPile));
    }

    private boolean isMoveValidInFoundationPile(Card card, Card topCardInPile, Pile destPile) {
        if (card.getRank().getValue() == (Ranks.ACE.getValue()) && destPile.numOfCards() == 0) {
            return true;
        } else if ((card.getRank().getValue() != (Ranks.ACE.getValue())) && destPile.numOfCards() == 0) {
            return false;
        }
        return (Card.isSameSuit(card, topCardInPile) && Card.isLower(topCardInPile, card));
    }

    private Pile getValidIntersectingPile(Card card, List<Pile> piles) {
        Pile result = null;
        for (Pile pile : piles) {
            if (!pile.equals(card.getContainingPile()) && isOverPile(card, pile) && isMoveValid(card, pile))
                result = pile;
        }
        return result;
    }

    private boolean isOverPile(Card card, Pile pile) {
        if (pile.isEmpty())
            return card.getBoundsInParent().intersects(pile.getBoundsInParent());
        else
            return card.getBoundsInParent().intersects(pile.getTopCard().getBoundsInParent());
    }

    private void handleValidMove(Card card, Pile destPile) {
        String msg = null;
        if (destPile.isEmpty()) {
            if (destPile.getPileType().equals(Pile.PileType.FOUNDATION))
                msg = String.format("Placed %s to the foundation.", card);
            if (destPile.getPileType().equals(Pile.PileType.TABLEAU))
                msg = String.format("Placed %s to a new pile.", card);
        } else {
            msg = String.format("Placed %s to %s.", card, destPile.getTopCard());
        }
        System.out.println(msg);
        MouseUtil.slideToDest(draggedCards, destPile);
        draggedCards.clear();
    }

    private void initPiles() {
        initStockPile();
        initDiscardPile();
        initFoundationPiles();
        initTableauPiles();
    }

    private void initStockPile() {
        stockPile = new Pile(Pile.PileType.STOCK, "Stock", STOCK_GAP);
        setDefaultForPile(stockPile, 95, 20);
        stockPile.setOnMouseClicked(stockReverseCardsHandler);
        getChildren().add(stockPile);
    }

    private void initDiscardPile() {
        discardPile = new Pile(Pile.PileType.DISCARD, "Discard", STOCK_GAP);
        setDefaultForPile(discardPile, 285, 20);
        getChildren().add(discardPile);
    }

    private void initFoundationPiles() {
        for (int i = 0; i < 4; i++) {
            Pile foundationPile = new Pile(Pile.PileType.FOUNDATION, "Foundation " + i, FOUNDATION_GAP);
            setDefaultForPile(foundationPile, 610 + i * 180, 20);
            foundationPiles.add(foundationPile);
            getChildren().add(foundationPile);
        }
    }

    private void initTableauPiles() {
        for (int i = 0; i < 7; i++) {
            Pile tableauPile = new Pile(Pile.PileType.TABLEAU, "Tableau " + i, TABLEAU_GAP);
            setDefaultForPile(tableauPile, 95 + i * 180, 275);
            tableauPiles.add(tableauPile);
            getChildren().add(tableauPile);
        }
    }

    private void setDefaultForPile(Pile pile, int layoutX, int layoutY) {
        pile.setBlurredBackground();
        pile.setLayoutX(layoutX);
        pile.setLayoutY(layoutY);
    }

    private void dealCards() {
        Collections.shuffle(deck);
        Iterator<Card> deckIterator = deck.iterator();
        dealCardsToTableauPiles(deckIterator);
        dealCardsToStockPile(deckIterator);

    }

    private void dealCardsToTableauPiles(Iterator<Card> deckIterator) {
        int cardsAmount = 1;
        for (Pile pile : tableauPiles) {
            for (int i = 0; i < cardsAmount; i++) {
                Card card = deckIterator.next();
                addMouseEventHandlers(card);
                getChildren().add(card);
                pile.addCard(card);
                if (i == cardsAmount - 1) {
                    card.flip();
                }
            }
            cardsAmount++;
        }
    }

    private void dealCardsToStockPile(Iterator<Card> deckIterator) {
        deckIterator.forEachRemaining(card -> {
            stockPile.addCard(card);
            addMouseEventHandlers(card);
            getChildren().add(card);
        });
    }

    void setTableBackground(Image tableBackground) {
        setBackground(new Background(new BackgroundImage(tableBackground, BackgroundRepeat.REPEAT,
                BackgroundRepeat.REPEAT, BackgroundPosition.CENTER, BackgroundSize.DEFAULT)));
    }

    void gameWon(Stage stage) {
        win = new Stage();
        win.setTitle("congratulations");
        button = new Button("Victory! Closing now...");
        button.setOnAction(e -> {
            this.attachedStage.close();
            win.close();
        });
        StackPane layout = new StackPane();
        layout.getChildren().add(button);
        Scene scene = new Scene(layout, 300, 250);
        win.setScene(scene);
        win.show();
    }
}
