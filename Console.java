package com.mygdx.game.Controllers;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.Array;
import com.mygdx.game.AssetContainers.Images;

public class GameConsole {
    private final SpriteBatch batch;
    private final ShapeRenderer shape;
    private final Matrix4 renderTransform;

    private boolean visible;
    private boolean active;
    private float y;
    private final float lineHeight;
    private int lineScroll;
    private final float screenPercentage = 0.65f;
    private final int xBorder = 42;

    private String currentIn;
    private String messageBin;
    private final Array<String> arguments;
    private final Array<String> textLines;
    private final String argPrefix = " @";
    private final String argSeparator= ",";

    private Color backgroundColor;
    private InputAdapter inputAdapter;

    public GameConsole() {
        active = false;
        visible = false;
        y=Gdx.graphics.getHeight();
        lineHeight = Images.font.getLineHeight()+8;
        lineScroll = 0;

        backgroundColor = new Color(0.15f, 0.6f, 0.25f, 0.3f);

        batch = new SpriteBatch();
        shape = new ShapeRenderer();
        renderTransform = new Matrix4();

        textLines = new Array<String>(25);
        currentIn = "";
        messageBin = "";
        arguments = new Array<String>(3);

        textLines.add("    -= welcome =-");

        createInputProcessor();
    }

    public Matrix4 getRenderTransform() {
        return renderTransform;
    }

    public void insertText(String str) {
        textLines.insert(0, str);
    }

    public String peekMessage() {
        return messageBin;
    }

    public String seeArgument(int index) {
        return arguments.get(index);
    }

    public InputAdapter getInputAdapter() {
        return inputAdapter;
    }

    public void clearMessage() {
        messageBin =  "";
        arguments.clear();
    }

    private void createInputProcessor() {
        inputAdapter = new InputAdapter() {
            @Override
            public boolean keyTyped (char character) {
                if (active &&
                        character != '\b' &&
                        character != '\n' &&
                        character != '\t' &&
                        character != '\r' &&
                        character != '\f' &&
                        character != '\\' &&
                        character != '`' &&
                        character != '~' )
                    currentIn += character;

                if (character == '\b') { //Backspace
                    currentIn = currentIn.substring(0, Math.max( 0, (currentIn+"MARKMARKMARK").indexOf("MARKMARKMARK")-1) );
                }

                if (character == '\t') {
                    messageBin = "edit";
                }
                return true;
            }

            @Override
            public boolean keyDown (int keycode) {
                if (active) {
                    switch(keycode) {
                        case Input.Keys.FORWARD_DEL: //'Delete' Key
                            currentIn = ""; //Bug fix to actually do this in update
                            break;
                        case Input.Keys.ENTER:
                            processInput();
                            break;
                        case Input.Keys.PAGE_DOWN:
                            lineScroll = Math.max(0, lineScroll - (int) (Gdx.graphics.getHeight()*screenPercentage/lineHeight) );
                            break;
                        case Input.Keys.PAGE_UP:
                            lineScroll = Math.min(
                                    (textLines.size+2) - (int) (screenPercentage*Gdx.graphics.getHeight()/lineHeight),
                                    lineScroll + (int) (Gdx.graphics.getHeight()*screenPercentage/lineHeight) );
                            break;
                        case Input.Keys.END:
                            lineScroll = 0;
                            break;
                        case Input.Keys.HOME:
                            lineScroll = (textLines.size+2) - (int) (screenPercentage*Gdx.graphics.getHeight()/lineHeight);
                        default:
                            break;
                    }
                }

                return true;
            }

            @Override
            public boolean scrolled(float amountX, float amountY) {
                lineScroll -= Math.round(amountY);
                if (lineScroll < 0) lineScroll = 0;

                float maxUp = (float)(textLines.size+3) - screenPercentage*Gdx.graphics.getHeight()/lineHeight;
                maxUp = Math.max(0.0f, maxUp);
                if (lineScroll > (int) maxUp) lineScroll = (int) maxUp;

                return true;
            }
        };
    }

    public void updateAndDraw(float delta) {
        //Fix bug where commands dont register currently after delete was pressed ?
        if (active && Gdx.input.isKeyJustPressed(Input.Keys.FORWARD_DEL) ) {
            currentIn = "";
        }

        //Toggle Active
        if ( Gdx.input.isKeyJustPressed(Input.Keys.GRAVE)) {
            active = !active;
        }

        //Slide console up or down
        float yGoal = Gdx.graphics.getHeight() * (1.0f - screenPercentage);
        if (active) {
            visible = true;
            if (y != yGoal)
                y =  Math.max(y-1500.0f*delta, yGoal);
        } else {
            if (y != Gdx.graphics.getHeight())
                y = Math.min(y+1500.0f*delta, Gdx.graphics.getHeight());
            if ( Math.round(y) == Gdx.graphics.getHeight())
                visible = false;
        }

        //Draw when not hidden
        if (active || y != Gdx.graphics.getHeight()) {
            draw();
        }
    }

    public void updateTransform() {
        renderTransform.setToOrtho2D(0.0f,0.0f, Gdx.graphics.getWidth(), Gdx.graphics.getHeight() );
    }

    public void setBackgroundColor(float r, float g, float b) {
        backgroundColor.set(r,g,b, backgroundColor.a);
    }

    private void processInput() {

        if (!currentIn.isEmpty()) {
            //Ensure previous arguments are cleared
            clearMessage();

            //Get Message and Arguments
            int argumentFillerIndex = 0;

            if (currentIn.contains(argPrefix)) {
                arguments.add(currentIn.substring(currentIn.indexOf(argPrefix) + 2) );
                currentIn = currentIn.substring(0, currentIn.indexOf(argPrefix));

                while (arguments.get(argumentFillerIndex).contains(argSeparator)) {
                    arguments.add(arguments.get(argumentFillerIndex).substring(arguments.get(argumentFillerIndex).indexOf(argSeparator) + 1) );
                    arguments.set( argumentFillerIndex, arguments.get(argumentFillerIndex).substring(0, arguments.get(argumentFillerIndex).indexOf(argSeparator)) );
                    argumentFillerIndex++;
                }
            }

            //Log Message and Arguments
            insertText(">> " + currentIn);

            if (!arguments.isEmpty()) {
                StringBuilder argLine = new StringBuilder("    >");
                for (String arg : arguments) {
                    if (!arg.isEmpty())
                        argLine.append(arg).append("  ");
                }
                insertText(argLine.toString());
            }

            //Drop Message and reset input
            currentIn = currentIn.toLowerCase();
            messageBin = currentIn.toLowerCase();
            currentIn = "";
        }
    }

    private void draw() {
        if (!visible) return;

        updateTransform();

        shape.begin(ShapeRenderer.ShapeType.Filled);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        shape.setProjectionMatrix(renderTransform);
        //Green Back
        shape.setColor(backgroundColor);
        shape.rect(0.0f,  y, Gdx.graphics.getWidth(), Gdx.graphics.getHeight()-y );
        //Entry Field
        shape.rect(0.0f,  y, Gdx.graphics.getWidth(), lineHeight );
        //Thin Line
        shape.setColor(1.0f, 1.0f, 0.9f, 0.5f);
        shape.rect(0.0f,  y-1, Gdx.graphics.getWidth(), 1 );
        shape.end();

        batch.begin();
        batch.setProjectionMatrix(renderTransform);
        Images.font.setColor(Color.WHITE);

        //Draw Fps
        Images.font.draw(batch, "FPS: " + String.valueOf((int)(1.0f/Gdx.graphics.getDeltaTime()) ), Gdx.graphics.getWidth()-175.0f, y+lineHeight-10.0f);

        //Draw Console Input
        Images.font.draw(batch, currentIn, xBorder, y - 5 + lineHeight);

        //Draw Console Log
        for (int i = lineScroll; i < textLines.size; i++) {
            Images.font.draw(batch, textLines.get(i), xBorder, y + (i+2-lineScroll)*lineHeight);
        }
        batch.end();
    }

}
