/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package lu.fisch.fifesoft;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.fife.ui.rsyntaxtextarea.RSyntaxDocument;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.Token;
import javax.swing.text.*;
import org.fife.ui.rsyntaxtextarea.RSyntaxUtilities;
import org.fife.ui.rsyntaxtextarea.TokenMakerFactory;

/**
 *
 * @author robertfisch
 */
public class RSyntaxTextAreaUIBackgroundDrawer_Copy
{
        private static void drawBox(RSyntaxTextArea textArea, Graphics g, Token startToken, Token headerToken, Token openToken, Token preCloseToken, Token closeToken)
        {
            try
            {
                
                if (closeToken!=null)
                {
                    int closePos = closeToken.getOffset();
                    int preClosePos = preCloseToken.getOffset();
                    
                    if(startToken!=null)
                    {
                        int startPos = startToken.getOffset();
                        int openPos = openToken.getOffset();

                        // define the outer rectangle
                        Rectangle filler = new Rectangle();
                        Rectangle startRect = ((RSyntaxTextArea)textArea).modelToView(startPos);
                        Rectangle openRect = ((RSyntaxTextArea)textArea).modelToView(openPos);
                        Rectangle closeRect = ((RSyntaxTextArea)textArea).modelToView(closePos);
                        filler.x=Math.min(startRect.x,closeRect.x);
                        filler.y=Math.min(startRect.y,closeRect.y);
                        filler.width=(startRect.x>closeRect.x?startRect.width:closeRect.width)+Math.abs(startRect.x-closeRect.x);
                        filler.height=(startRect.y>closeRect.y?startRect.height:closeRect.height)+Math.abs(startRect.y-closeRect.y);
                        // draw the background

                        if(headerToken.getLexeme().trim().equals("switch"))
                            g.setColor(new Color(244,244,251));
                        if(headerToken.getLexeme().trim().equals("if"))
                            g.setColor(new Color(244,244,251));
                        if(headerToken.getLexeme().trim().equals("try"))
                            g.setColor(new Color(255,230,230));
                        if(headerToken.getLexeme().trim().equals("else"))
                            g.setColor(new Color(244,244,251));
                        if(headerToken.getLexeme().trim().equals("for"))
                            g.setColor(new Color(244,251,251));
                        if(headerToken.getLexeme().trim().equals("while"))
                            g.setColor(new Color(251,244,251));
                        if(headerToken.getLexeme().trim().equals("do"))
                            g.setColor(new Color(251,244,251));
                        if(headerToken.getLexeme().trim().equals("public"))
                            g.setColor(new Color(252,252,217));
                        if(headerToken.getLexeme().trim().equals("private"))
                            g.setColor(new Color(252,252,217));
                        if(headerToken.getNextToken().getLexeme().trim().equals("class"))
                            g.setColor(new Color(240,251,240));
                        //System.out.println(headerToken.getLexeme()+" => "+headerToken.getNextToken().getLexeme());

                        int w = -5;
                        g.fillRoundRect(filler.x+w,filler.y-1, ((RSyntaxTextArea)textArea).getWidth()-w,filler.height+2,10,10);
                        // draw the line
                        g.setColor(new Color(235,235,230));
                        g.drawRoundRect(filler.x+w,filler.y-1, ((RSyntaxTextArea)textArea).getWidth()-w,filler.height+2,10,10);
                    }
                    
                    // define the inner rectangle
                    if(openToken.getNextToken().getOffset()!=closeToken.getOffset())
                    {
                        Rectangle filler = new Rectangle();
                        Rectangle openRect = ((RSyntaxTextArea)textArea).modelToView(openToken.getNextToken().getOffset());
                        Rectangle closeRect = ((RSyntaxTextArea)textArea).modelToView(preClosePos);
                        filler.x=Math.min(openRect.x,closeRect.x);
                        filler.y=Math.min(openRect.y,closeRect.y);
                        filler.width=(openRect.x>closeRect.x?openRect.width:closeRect.width)+Math.abs(openRect.x-closeRect.x);
                        filler.height=(openRect.y>closeRect.y?openRect.height:closeRect.height)+Math.abs(openRect.y-closeRect.y);
                        int lh = ((RSyntaxTextArea)textArea).getLineHeight();
                        int w = -5;
                        if(openToken.getNextToken().getOffset()==closeToken.getOffset()) 
                        {
                        }
                        if(filler.height-lh==0)
                        {
                            filler.height=lh;
                        }
                        // draw the background
                        g.setColor(Color.WHITE);
                        g.fillRoundRect(filler.x+w,filler.y-1, ((RSyntaxTextArea)textArea).getWidth()-w,filler.height+2,10,10);
                        // draw the line
                        g.setColor(new Color(235,235,230));
                        g.drawRoundRect(filler.x+w,filler.y-1, ((RSyntaxTextArea)textArea).getWidth()-w,filler.height+2,10,10);
                    }
                }
            }
            catch(Exception ex)
            {
                ex.printStackTrace();
            }
        }
        
        public static void paintBackground(Graphics g, RSyntaxTextArea textArea)
        {
           // list to store all the tokens
            ArrayList<Token> tokens = new ArrayList<Token>();
            // done offsets
            ArrayList<Integer> doneOffsets = new ArrayList<Integer>();
            // get a reference to the document
            RSyntaxDocument doc = (RSyntaxDocument)textArea.getDocument();
            // get a reference to the map
            Element map = doc.getDefaultRootElement();
            // loop through all the lines
            Token lastToken = null;
            for(int line=0;line<map.getElementCount();line++)
            {
                // get the first token for this list
                Token token = doc.getTokenListForLine(line);
                // loop through all tokens of this line
                while(token!=null)
                {
                    // only select the tokens that do contain text
                    if(token.toString()!=null)
                    {
                        /*
                        // ignore full whitespace tokens and some other
                        if(!token.getLexeme().trim().equals("") && 
                           token.getType()!=Token.NULL &&
                           token.getType()!=Token.COMMENT_EOL &&
                           token.getType()!=Token.COMMENT_MULTILINE
                          )
                        {
                            // make a copy of this token
                            Token copy = new DefaultToken();
                            copy.copyFrom(token);
                            // link the previous token to this token
                            if(lastToken!=null)
                                lastToken.setNextToken(copy);
                            //String tokenText = token.getLexeme();
                            tokens.add(copy);
                            // remember last token
                            lastToken = copy;
                        }
                        */
                        
                        tokens.add(token);
                    }
                    // goto the next token
                    token=token.getNextToken();
                }
            }
            
            // now we can loop through the tokens
            for(int t=0;t<tokens.size();t++)
            {
                Token startToken = null;
                Token headerToken = null;
                Token openToken = null;
                Token preCloseToken = null;
                Token closeToken = null;

                // get the current token
                Token token = tokens.get(t);
                String tokenText = token.getLexeme();
                // check for JavaDoc
                if (tokenText.trim().startsWith("/**"))
                {
                    // remeber this
                    startToken = token;
                    // move to end of JavaDoc
                    do
                    {
                        token=token.getNextToken();
                        tokenText = token.getLexeme();
                    }
                    while(token!=null && 
                          !tokenText.trim().startsWith("*/"));
                    if (token!=null)
                    {
                        // skip empty tokens
                        while(tokenText.trim().equals(""))
                        {
                            token=token.getNextToken();
                            tokenText = token.getLexeme();
                        }
                        // remember
                        token = token.getNextToken();
                        headerToken = token;
                        doneOffsets.add(headerToken.getOffset());
                        // goto open bracket
                        do
                        {
                            token=token.getNextToken();
                            tokenText = token.getLexeme();
                            // if we find something unsual, stop immediately
                            if(tokenText.trim().startsWith("/**") ||
                               tokenText.trim().startsWith("public") ||
                               tokenText.trim().startsWith("private"))
                            {
                                token = null;
                                break;
                            }
                        }
                        while(token!=null &&
                              !tokenText.trim().equals("{"));
                        // test
                        if(token!=null)
                        {
                            // remember
                            openToken = token;

                            int closePos = getMatchingBracketPosition((RSyntaxTextArea)textArea,openToken.getOffset());
                            int closeLine = map.getElementIndex(closePos);
                            closeToken = doc.getTokenListForLine(closeLine);
                            closeToken = RSyntaxUtilities.getTokenAtOffset(closeToken, closePos);  
                            if(closeToken!=null)
                            {
                                // search previous token
                                for(int i=1;i<tokens.size();i++)
                                {
                                    if(tokens.get(i).getOffset()==closeToken.getOffset())
                                        preCloseToken = tokens.get(i-1);
                                }

                                if(token!=null)
                                {
                                    // find matching closing bracket
                                    drawBox(textArea, g, startToken, headerToken, openToken, preCloseToken, closeToken);
                                }
                            }
                        }
                    }
                }
                // check for method
                // %TODO% improve this detection
                else if (
                            tokenText.trim().equals("public") || tokenText.trim().equals("private")
                        )
                {
                    // remeber this
                    startToken = token;
                    headerToken = token;
                    if(!doneOffsets.contains(headerToken.getOffset()))
                    {
                        // goto open bracket
                        do
                        {
                            // move on
                            token=token.getNextToken();
                            if(token!=null)
                                if(token.toString()!=null)
                                {    
                                    tokenText = token.getLexeme();
                                    // if we find something unsual, stop immediately
                                    if(tokenText.trim().startsWith("/**") ||
                                       tokenText.trim().startsWith("public") ||
                                       tokenText.trim().startsWith("private"))
                                    {
                                        token = null;
                                        break;
                                    }
                                }
                        }
                        while(token!=null &&
                              !tokenText.trim().equals("{"));
                        // test
                        if(token!=null)
                        {
                            // remember
                            openToken = token;

                            int closePos = getMatchingBracketPosition((RSyntaxTextArea)textArea,openToken.getOffset());
                            int closeLine = map.getElementIndex(closePos);
                            closeToken = doc.getTokenListForLine(closeLine);
                            closeToken = RSyntaxUtilities.getTokenAtOffset(closeToken, closePos);                        
                            
                            if(closeToken!=null)
                            {
                                // search previous token
                                for(int i=1;i<tokens.size();i++)
                                {
                                    if(tokens.get(i).getOffset()==closeToken.getOffset())
                                        preCloseToken = tokens.get(i-1);
                                }


                                if(token!=null)
                                {
                                    // find matching closing bracket
                                    drawBox(textArea, g, startToken, headerToken, openToken, preCloseToken, closeToken);
                                }
                            }
                        }
                    }
                }
                // check for "for" & "while"
                else if (
                            tokenText.trim().equals("for") || 
                            tokenText.trim().equals("while") ||
                            tokenText.trim().equals("switch")
                        )
                {
                    // remeber this
                    startToken = token;
                    headerToken = token;
                    if(!doneOffsets.contains(headerToken.getOffset()))
                    {
                        // the next token should be a "("
                        token=token.getNextToken();
                        // now we need to find the corresponding closing bracket
                        int closeOpenPos = getMatchingBracketPosition((RSyntaxTextArea)textArea,token.getOffset());
                        int closeOpenLine = map.getElementIndex(closeOpenPos);
                        Token closeOpenToken = doc.getTokenListForLine(closeOpenLine);
                        closeOpenToken = RSyntaxUtilities.getTokenAtOffset(closeOpenToken, closeOpenPos); 
                        if(closeOpenToken!=null)
                        {
                            // we need now to get "our" token copy
                            for(int i=0;i<tokens.size();i++)
                                if(tokens.get(i).getOffset()==closeOpenToken.getOffset())
                                    token=tokens.get(i);
                            // the next token should be a "{"
                            Token previousOpen = token;
                            token = token.getNextToken();                        
                            // test
                            if(token!=null)
                            {
                                // remember
                                openToken = token;

                                if(token.getLexeme().equals("{"))
                                {
                                    int closePos = getMatchingBracketPosition((RSyntaxTextArea)textArea,openToken.getOffset());
                                    int closeLine = map.getElementIndex(closePos);
                                    closeToken = doc.getTokenListForLine(closeLine);
                                    closeToken = RSyntaxUtilities.getTokenAtOffset(closeToken, closePos);                        
                                    // search previous token
                                    for(int i=1;i<tokens.size();i++)
                                    {
                                        if(tokens.get(i).getOffset()==closeToken.getOffset())
                                            preCloseToken = tokens.get(i-1);
                                    }

                                    if(token!=null)
                                    {
                                        // find matching closing bracket
                                        drawBox(textArea, g, startToken, headerToken, openToken, preCloseToken, closeToken);
                                    }
                                }
                                else if(!token.getLexeme().equals(";"))
                                {
                                    // move to the next ";"
                                    do
                                    {
                                        token=token.getNextToken();
                                        if(token!=null)
                                            if(token.toString()!=null)
                                            {
                                                tokenText = token.getLexeme();
                                                // if we find something unsual, stop immediately
                                                if(tokenText.trim().startsWith("/**") ||
                                                   tokenText.trim().startsWith("public") ||
                                                   tokenText.trim().startsWith("private"))
                                                {
                                                    token = null;
                                                    break;
                                                }
                                            }
                                    }
                                    while(token!=null &&
                                          !tokenText.trim().equals(";"));
                                    // remmeber
                                    closeToken = token;
                                    if(token!=null)
                                    {
                                        drawBox(textArea, g, startToken, headerToken, previousOpen, closeToken, closeToken);
                                    }
                                }
                            } 
                        }
                    }
                }
                // check for "if"
                else if (tokenText.trim().equals("if"))
                {
                    // remeber this
                    startToken = token;
                    headerToken = token;
                    if(!doneOffsets.contains(headerToken.getOffset()))
                    {
                        // the next token should be a "("
                        token=token.getNextToken();
                        // now we need to find the corresponding closing bracket
                        int closeOpenPos = getMatchingBracketPosition((RSyntaxTextArea)textArea,token.getOffset());
                        int closeOpenLine = map.getElementIndex(closeOpenPos);
                        Token closeOpenToken = doc.getTokenListForLine(closeOpenLine);
                        closeOpenToken = RSyntaxUtilities.getTokenAtOffset(closeOpenToken, closeOpenPos); 
                        if(closeOpenToken!=null)
                        {
                            // we need now to get "our" token copy
                            for(int i=0;i<tokens.size();i++)
                                if(tokens.get(i).getOffset()==closeOpenToken.getOffset())
                                    token=tokens.get(i);
                            // the next token should be a "{"
                            Token previousOpen = token;
                            token = token.getNextToken();                        
                            // test
                            if(token!=null)
                            {
                                // remember
                                openToken = token;

                                if(token.getLexeme().equals("{"))
                                {
                                    int closePos = getMatchingBracketPosition((RSyntaxTextArea)textArea,openToken.getOffset());
                                    int closeLine = map.getElementIndex(closePos);
                                    closeToken = doc.getTokenListForLine(closeLine);
                                    closeToken = RSyntaxUtilities.getTokenAtOffset(closeToken, closePos);                        
                                    // search previous token
                                    for(int i=1;i<tokens.size();i++)
                                    {
                                        if(tokens.get(i).getOffset()==closeToken.getOffset())
                                        {
                                            closeToken=tokens.get(i);
                                            preCloseToken = tokens.get(i-1);
                                            break;
                                        }
                                    }
                                    
                                    // find matching closing bracket
                                    drawBox(textArea, g, startToken, headerToken, openToken, preCloseToken, closeToken);

                                    if(closeToken.getNextToken().getLexeme().trim().equals("else"))
                                    {
                                        Token previousOpen2 = closeToken.getNextToken();
                                        Token tmp = closeToken.getNextToken().getNextToken();
                                        
                                        
                                        if(tmp.getLexeme().equals("{"))
                                        {
                                            closePos = getMatchingBracketPosition((RSyntaxTextArea)textArea,tmp.getOffset());
                                            closeLine = map.getElementIndex(closePos);
                                            closeToken = doc.getTokenListForLine(closeLine);
                                            closeToken = RSyntaxUtilities.getTokenAtOffset(closeToken, closePos);                        
                                            // search previous token
                                            Token preClose2Token = null;
                                            for(int i=1;i<tokens.size();i++)
                                            {
                                                if(tokens.get(i).getOffset()==closeToken.getOffset())
                                                {
                                                    closeToken=tokens.get(i);
                                                    preClose2Token = tokens.get(i-1);
                                                    break;
                                                }
                                            }
                                            // draw the big blue box and the white box of the else
                                            drawBox(textArea, g, startToken, headerToken, tmp, preClose2Token, closeToken);
                                            // draw the white box of the if
                                            drawBox(textArea, g, null, headerToken, openToken, preCloseToken, closeToken);
                                        }
                                        else if(!tmp.getLexeme().equals(";"))
                                        {
                                            token = tmp;
                                            // move to the next ";"
                                            do
                                            {
                                                token=token.getNextToken();
                                                if(token!=null)
                                                    if(token.toString()!=null)
                                                    {
                                                        tokenText = token.getLexeme();
                                                        // if we find something unsual, stop immediately
                                                        if(tokenText.trim().startsWith("/**") ||
                                                           tokenText.trim().startsWith("public") ||
                                                           tokenText.trim().startsWith("private"))
                                                        {
                                                            token = null;
                                                            break;
                                                        }
                                                    }
                                            }
                                            while(token!=null &&
                                                  !tokenText.trim().equals(";"));
                                            // remmeber
                                            Token closeToken2 = token;
                                            if(closeToken!=null)
                                            {
                                                // draw the big blue box and the white box of the else
                                                drawBox(textArea, g, startToken, headerToken, previousOpen2, closeToken2, closeToken2);
                                                // draw the white box of the if
                                                drawBox(textArea, g, null, headerToken, openToken, preCloseToken, closeToken);
                                            }
                                        }
                                    }
                                    else
                                    {
                                        drawBox(textArea, g, startToken, headerToken, openToken, preCloseToken, closeToken);
                                    }

                                }
                                else if(!token.getLexeme().equals(";"))
                                {
                                    // move to the next ";"
                                    do
                                    {
                                        token=token.getNextToken();
                                        if(token!=null)
                                            if(token.toString()!=null)
                                            {
                                                tokenText = token.getLexeme();
                                                // if we find something unsual, stop immediately
                                                if(tokenText.trim().startsWith("/**") ||
                                                   tokenText.trim().startsWith("public") ||
                                                   tokenText.trim().startsWith("private"))
                                                {
                                                    token = null;
                                                    break;
                                                }
                                            }
                                    }
                                    while(token!=null &&
                                          !tokenText.trim().equals(";"));
                                    // remmeber
                                    closeToken = token;
                                    if(closeToken!=null)
                                    {
                                        if(closeToken.getNextToken().getLexeme().trim().equals("else"))
                                        {                                        
                                            Token previousOpen2 = closeToken.getNextToken();
                                            Token tmp = closeToken.getNextToken().getNextToken();


                                            if(tmp.getLexeme().equals("{"))
                                            {
                                                int closePos = getMatchingBracketPosition((RSyntaxTextArea)textArea,tmp.getOffset());
                                                int closeLine = map.getElementIndex(closePos);
                                                Token closeTokenO = doc.getTokenListForLine(closeLine);
                                                closeTokenO = RSyntaxUtilities.getTokenAtOffset(closeTokenO, closePos);         
                                                // search previous token
                                                Token preClose2Token = null;
                                                for(int i=1;i<tokens.size();i++)
                                                {
                                                    if(tokens.get(i).getOffset()==closeTokenO.getOffset())
                                                    {
                                                        closeTokenO=tokens.get(i);
                                                        preClose2Token = tokens.get(i-1);
                                                        break;
                                                    }
                                                }
                                                // draw the big blue box and the white box of the else
                                                drawBox(textArea, g, startToken, headerToken, tmp, preClose2Token, closeTokenO);
                                                // draw the white box of the if
                                                drawBox(textArea, g, null, headerToken, previousOpen, closeToken, closeTokenO);
                                            }
                                            else if(!tmp.getLexeme().equals(";"))
                                            {
                                                token = tmp;
                                                // move to the next ";"
                                                do
                                                {
                                                    token=token.getNextToken();
                                                    if(token!=null)
                                                        if(token.toString()!=null)
                                                        {
                                                            tokenText = token.getLexeme();
                                                            // if we find something unsual, stop immediately
                                                            if(tokenText.trim().startsWith("/**") ||
                                                               tokenText.trim().startsWith("public") ||
                                                               tokenText.trim().startsWith("private"))
                                                            {
                                                                token = null;
                                                                break;
                                                            }
                                                        }
                                                }
                                                while(token!=null &&
                                                      !tokenText.trim().equals(";"));
                                                // remmeber
                                                Token closeToken2 = token;
                                                if(closeToken!=null)
                                                {
                                                    // draw the big blue box and the white box of the else
                                                    drawBox(textArea, g, startToken, headerToken, previousOpen2, closeToken2, closeToken2);
                                                    // draw the white box of the if
                                                    drawBox(textArea, g, null, headerToken, previousOpen, closeToken, closeToken);
                                                }
                                            }
                                        }
                                        else
                                        {
                                            drawBox(textArea, g, startToken, headerToken, previousOpen, closeToken, closeToken);
                                        }
                                    }
                                }
                            } 
                        }
                    }
                }
                 // check for "try"
                else if (tokenText.trim().equals("try"))
                {
                    // remeber this
                    startToken = token;
                    headerToken = token;
                    if(!doneOffsets.contains(headerToken.getOffset()))
                    {
                        // the next token should be a "{"
                        openToken=token.getNextToken();
                        if(openToken!=null)
                            if(openToken.toString()!=null)
                                if(openToken.getLexeme().equals("{"))
                                {
                                    int closePos = getMatchingBracketPosition((RSyntaxTextArea)textArea,openToken.getOffset());
                                    int closeLine = map.getElementIndex(closePos);
                                    closeToken = doc.getTokenListForLine(closeLine);
                                    closeToken = RSyntaxUtilities.getTokenAtOffset(closeToken, closePos);                        
                                    // search previous token
                                    for(int i=1;i<tokens.size();i++)
                                    {
                                        if(tokens.get(i).getOffset()==closeToken.getOffset())
                                        {
                                            closeToken=tokens.get(i);
                                            preCloseToken = tokens.get(i-1);
                                            break;
                                        }
                                    }
                                    
                                    // find matching closing bracket
                                    drawBox(textArea, g, startToken, headerToken, openToken, preCloseToken, closeToken);

                                    if(closeToken.getNextToken().getLexeme().trim().equals("finally"))
                                    {
                                        Token previousOpen2 = closeToken.getNextToken();
                                        Token tmp = closeToken.getNextToken().getNextToken();
                                        
                                        
                                        if(tmp.getLexeme().equals("{"))
                                        {
                                            closePos = getMatchingBracketPosition((RSyntaxTextArea)textArea,tmp.getOffset());
                                            closeLine = map.getElementIndex(closePos);
                                            closeToken = doc.getTokenListForLine(closeLine);
                                            closeToken = RSyntaxUtilities.getTokenAtOffset(closeToken, closePos);                        
                                            // search previous token
                                            Token preClose2Token = null;
                                            for(int i=1;i<tokens.size();i++)
                                            {
                                                if(tokens.get(i).getOffset()==closeToken.getOffset())
                                                {
                                                    closeToken=tokens.get(i);
                                                    preClose2Token = tokens.get(i-1);
                                                    break;
                                                }
                                            }
                                            // draw the big blue box and the white box of the else
                                            drawBox(textArea, g, startToken, headerToken, tmp, preClose2Token, closeToken);
                                            // draw the white box of the if
                                            drawBox(textArea, g, null, headerToken, openToken, preCloseToken, closeToken);
                                        }
                                    }
                                    else if(closeToken.getNextToken().getLexeme().trim().equals("catch"))
                                    {
                                        // the next token should be a "("
                                        token=closeToken.getNextToken().getNextToken();
                                        // now we need to find the corresponding closing bracket
                                        int closeOpenPos = getMatchingBracketPosition((RSyntaxTextArea)textArea,token.getOffset());
                                        int closeOpenLine = map.getElementIndex(closeOpenPos);
                                        Token closeOpenToken = doc.getTokenListForLine(closeOpenLine);
                                        closeOpenToken = RSyntaxUtilities.getTokenAtOffset(closeOpenToken, closeOpenPos); 
                                        if(closeOpenToken!=null)
                                        {
                                           // search previous token
                                            Token preClose2Token = null;
                                            for(int i=1;i<tokens.size();i++)
                                            {
                                                if(tokens.get(i).getOffset()==closeOpenToken.getOffset())
                                                {
                                                    closeOpenToken=tokens.get(i);
                                                    preClose2Token = tokens.get(i-1);
                                                    break;
                                                }
                                            }
                                        }
                                        
                                        Token previousOpen2 = closeOpenToken;
                                        if(closeOpenToken!=null)
                                        {
                                            Token tmp = closeOpenToken.getNextToken();

                                            if(tmp.getLexeme().equals("{"))
                                            {
                                                closePos = getMatchingBracketPosition((RSyntaxTextArea)textArea,tmp.getOffset());
                                                closeLine = map.getElementIndex(closePos);
                                                closeToken = doc.getTokenListForLine(closeLine);
                                                closeToken = RSyntaxUtilities.getTokenAtOffset(closeToken, closePos);                        
                                                // search previous token
                                                Token preClose2Token = null;
                                                for(int i=1;i<tokens.size();i++)
                                                {
                                                    if(tokens.get(i).getOffset()==closeToken.getOffset())
                                                    {
                                                        closeToken=tokens.get(i);
                                                        preClose2Token = tokens.get(i-1);
                                                        break;
                                                    }
                                                }

                                                if (closeToken.getNextToken().getLexeme().equals("finally"))
                                                {
                                                    Token tmp2 = closeToken.getNextToken().getNextToken();

                                                    closePos = getMatchingBracketPosition((RSyntaxTextArea)textArea,tmp2.getOffset());
                                                    closeLine = map.getElementIndex(closePos);
                                                    closeToken = doc.getTokenListForLine(closeLine);
                                                    closeToken = RSyntaxUtilities.getTokenAtOffset(closeToken, closePos);                        
                                                    if(closeToken!=null)
                                                    {
                                                        // search previous token
                                                        Token preClose3Token = null;
                                                        for(int i=1;i<tokens.size();i++)
                                                        {
                                                            if(tokens.get(i).getOffset()==closeToken.getOffset())
                                                            {
                                                                closeToken=tokens.get(i);
                                                                preClose3Token = tokens.get(i-1);
                                                                break;
                                                            }
                                                        }

                                                        // draw the big blue box and the white box of the else
                                                        drawBox(textArea, g, startToken, headerToken, tmp2, preClose3Token, closeToken);
                                                        // draw the big blue box and the white box of the else
                                                        drawBox(textArea, g, null, headerToken, tmp, preClose2Token, closeToken);
                                                        // draw the white box of the if
                                                        drawBox(textArea, g, null, headerToken, openToken, preCloseToken, closeToken);
                                                    }
                                                }
                                                else
                                                {
                                                    // draw the big blue box and the white box of the else
                                                    drawBox(textArea, g, startToken, headerToken, tmp, preClose2Token, closeToken);
                                                    // draw the white box of the if
                                                    drawBox(textArea, g, null, headerToken, openToken, preCloseToken, closeToken);
                                                }
                                            }
                                        }
                                    }
                                    else
                                    {
                                        drawBox(textArea, g, startToken, headerToken, openToken, preCloseToken, closeToken);
                                    }
                                }
                    }
                }
                // check for "do ... while"
                else if (tokenText.trim().equals("do"))
                {
                    // remeber this
                    startToken = token;
                    headerToken = token;
                    if(!doneOffsets.contains(headerToken.getOffset()))
                    {
                        // the next token should be a "{"
                        Token previousOpen=token;
                        openToken=token.getNextToken();
                        if(openToken.getLexeme().equals("{"))
                        {
                            int closePos = getMatchingBracketPosition((RSyntaxTextArea)textArea,openToken.getOffset());
                            int closeLine = map.getElementIndex(closePos);
                            closeToken = doc.getTokenListForLine(closeLine);
                            closeToken = RSyntaxUtilities.getTokenAtOffset(closeToken, closePos);                        
                            // search previous token
                            for(int i=1;i<tokens.size();i++)
                            {
                                if(tokens.get(i).getOffset()==closeToken.getOffset())
                                    preCloseToken = tokens.get(i-1);
                            }
                            // goto ";"
                            closeToken=preCloseToken.getNextToken();
                            while(closeToken.toString()!=null && !closeToken.getLexeme().equals(";"))
                                closeToken=closeToken.getNextToken();

                            if(closeToken!=null && closeToken.toString()!=null)
                            {
                                // find matching closing bracket
                                drawBox(textArea, g, startToken, headerToken, openToken, preCloseToken, closeToken);
                            }
                        }
                    }
                }
            }
        }
    
        /**
         * source: org.fife.ui.rsyntaxtextarea.RSyntaxUtilities
         */
	public static int getMatchingBracketPosition(RSyntaxTextArea textArea, int caretPosition) 
        {
                Segment charSegment = new Segment();

                
			// Actually position just BEFORE caret.
			//int caretPosition = textArea.getCaretPosition() - 1;
			if (caretPosition>-1) {
                    try {
                        // Some variables that will be used later.
                        Token token;
                        Element map;
                        int curLine;
                        Element line;
                        int start, end;
                        RSyntaxDocument doc = (RSyntaxDocument)textArea.getDocument();
                        char bracket  = doc.getText(caretPosition, 1).charAt(0);

                        // First, see if the previous char was a bracket
                        // ('{', '}', '(', ')', '[', ']').
                        // If it was, then make sure this bracket isn't sitting in
                        // the middle of a comment or string.  If it isn't, then
                        // initialize some stuff so we can continue on.
                        char bracketMatch;
                        boolean goForward;
                        switch (bracket) {

                                case '{':
                                case '(':

                                        // Ensure this bracket isn't in a comment.
                                        map = doc.getDefaultRootElement();
                                        curLine = map.getElementIndex(caretPosition);
                                        line = map.getElement(curLine);
                                        start = line.getStartOffset();
                                        end = line.getEndOffset();
                                        token = doc.getTokenListForLine(curLine);
                                        token = RSyntaxUtilities.getTokenAtOffset(token, caretPosition);
                                        // All brackets are always returned as "separators."
                                        if (token.getType()!=Token.SEPARATOR) {
                                                return -1;
                                        }
                                        bracketMatch = bracket=='{' ? '}' : (bracket=='(' ? ')' : ']');
                                        goForward = true;
                                        break;

                                case '}':
                                case ')':

                                        // Ensure this bracket isn't in a comment.
                                        map = doc.getDefaultRootElement();
                                        curLine = map.getElementIndex(caretPosition);
                                        line = map.getElement(curLine);
                                        start = line.getStartOffset();
                                        end = line.getEndOffset();
                                        token = doc.getTokenListForLine(curLine);
                                        token = RSyntaxUtilities.getTokenAtOffset(token, caretPosition);
                                        // All brackets are always returned as "separators."
                                        if (token.getType()!=Token.SEPARATOR) {
                                                return -1;
                                        }
                                        bracketMatch = bracket=='}' ? '{' : (bracket==')' ? '(' : '[');
                                        goForward = false;
                                        break;

                                default:
                                        return -1;

                        }

                        if (goForward) {

                                int lastLine = map.getElementCount();

                                // Start just after the found bracket since we're sure
                                // we're not in a comment.
                                start = caretPosition + 1;
                                int numEmbedded = 0;
                                boolean haveTokenList = false;

                                while (true) {
                                    try {
                                        doc.getText(start,end-start, charSegment);
                                        int segOffset = charSegment.offset;

                                        for (int i=segOffset; i<segOffset+charSegment.count; i++) {

                                                char ch = charSegment.array[i];

                                                if (ch==bracket) {
                                                        if (haveTokenList==false) {
                                                                token = doc.getTokenListForLine(curLine);
                                                                haveTokenList = true;
                                                        }
                                                        int offset = start + (i-segOffset);
                                                        token = RSyntaxUtilities.getTokenAtOffset(token, offset);
                                                        if (token.getType()==Token.SEPARATOR)
                                                                numEmbedded++;
                                                }

                                                else if (ch==bracketMatch) {
                                                        if (haveTokenList==false) {
                                                                token = doc.getTokenListForLine(curLine);
                                                                haveTokenList = true;
                                                        }
                                                        int offset = start + (i-segOffset);
                                                        token = RSyntaxUtilities.getTokenAtOffset(token, offset);
                                                        if (token.getType()==Token.SEPARATOR) {
                                                                if (numEmbedded==0) {
                                                                        if (textArea.isCodeFoldingEnabled() &&
                                                                                        textArea.getFoldManager().isLineHidden(curLine)) {
                                                                                return -1; // Match hidden in a fold
                                                                        }
                                                                        return offset;
                                                                }
                                                                numEmbedded--;
                                                        }
                                                }

                                        } // End of for (int i=segOffset; i<segOffset+charSegment.count; i++).

                                        // Bail out if we've gone through all lines and
                                        // haven't found the match.
                                        if (++curLine==lastLine)
                                                return -1;

                                        // End of while (true).
                                        haveTokenList = false;
                                        line = map.getElement(curLine);
                                        start = line.getStartOffset();
                                        end = line.getEndOffset();
                                    } // End of while (true).
                                    catch (BadLocationException ex) {
                                        return -1;
                                    }

                                } // End of if (goForward).

                        } // End of if (goForward).


                        // Otherwise, we're going backward through the file
                        // (since we found '}', ')' or ']').
                        else {	// goForward==false

                                // End just before the found bracket since we're sure
                                // we're not in a comment.
                                end = caretPosition;// - 1;
                                int numEmbedded = 0;
                                boolean haveTokenList = false;
                                Token t2;

                                while (true) {
                                    try {
                                        doc.getText(start,end-start, charSegment);
                                        int segOffset = charSegment.offset;
                                        int iStart = segOffset + charSegment.count - 1;

                                        for (int i=iStart; i>=segOffset; i--) {

                                                char ch = charSegment.array[i];

                                                if (ch==bracket) {
                                                        if (haveTokenList==false) {
                                                                token = doc.getTokenListForLine(curLine);
                                                                haveTokenList = true;
                                                        }
                                                        int offset = start + (i-segOffset);
                                                        t2 = RSyntaxUtilities.getTokenAtOffset(token, offset);
                                                        if (t2.getType()==Token.SEPARATOR)
                                                                numEmbedded++;
                                                }

                                                else if (ch==bracketMatch) {
                                                        if (haveTokenList==false) {
                                                                token = doc.getTokenListForLine(curLine);
                                                                haveTokenList = true;
                                                        }
                                                        int offset = start + (i-segOffset);
                                                        t2 = RSyntaxUtilities.getTokenAtOffset(token, offset);
                                                        if (t2.getType()==Token.SEPARATOR) {
                                                                if (numEmbedded==0)
                                                                        return offset;
                                                                numEmbedded--;
                                                        }
                                                }

                                        }

                                        // Bail out if we've gone through all lines and
                                        // haven't found the match.
                                        if (--curLine==-1)
                                                return -1;

                                        // End of while (true).
                                        // next line.
                                        haveTokenList = false;
                                        line = map.getElement(curLine);
                                        start = line.getStartOffset();
                                        end = line.getEndOffset();
                                    } // End of while (true).
                                    catch (BadLocationException ex) {
                                        return -1;
                                    }

                                } // End of else.

                        } // End of else.
                    } // End of if (caretPosition>-1).
                    catch (BadLocationException ex) {
                        return -1;
                    }

			} // End of if (caretPosition>-1).


		// Something went wrong...
		return -1;

	}

}
