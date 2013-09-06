/*
GanttProject is an opensource project management tool.
Copyright (C) 2009 Dmitry Barashev

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 3
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.ganttproject.impex.htmlpdf.fonts;

import java.awt.Font;
import java.awt.FontFormatException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.logging.Level;

import org.eclipse.core.runtime.Platform;
import org.ganttproject.impex.htmlpdf.itext.FontSubstitutionModel;
import org.ganttproject.impex.htmlpdf.itext.FontSubstitutionModel.FontSubstitution;
import org.ganttproject.impex.htmlpdf.itext.ITextEngine;

import com.google.common.base.Function;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.itextpdf.awt.FontMapper;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.pdf.BaseFont;

import net.sourceforge.ganttproject.GPLogger;
import net.sourceforge.ganttproject.language.GanttLanguage;

/**
 * This class collects True Type fonts from .ttf files in the registered
 * directories and provides mappings of font family names to plain AWT fonts and
 * iText fonts.
 *
 * @author dbarashev
 */
public class TTFontCache {
  private static String FALLBACK_FONT_PATH = "/fonts/LiberationSans-Regular.ttf";
  private Map<String, Supplier<Font>> myMap_Family_RegularFont = new TreeMap<String, Supplier<Font>>();
  private final Map<FontKey, com.itextpdf.text.Font> myFontCache = new HashMap<FontKey, com.itextpdf.text.Font>();
  private Map<String, Function<String, BaseFont>> myMap_Family_ItextFont = new HashMap<String, Function<String, BaseFont>>();
  private Properties myProperties;
  private BaseFont myFallbackFont;

  public void registerDirectory(String path) {
    GPLogger.getLogger(getClass()).fine("reading directory=" + path);
    File dir = new File(path);
    if (dir.exists() && dir.isDirectory()) {
      registerFonts(dir);
    } else {
      GPLogger.getLogger(getClass()).fine("directory " + path + " is not readable");
    }
  }

  public List<String> getRegisteredFamilies() {
    return new ArrayList<String>(myMap_Family_RegularFont.keySet());
  }

  public Font getAwtFont(String family) {
    Supplier<Font> supplier = myMap_Family_RegularFont.get(family);
    return supplier == null ? null : supplier.get();
  }

  private void registerFonts(File dir) {
    boolean runningUnderJava6;
    try {
      Font.class.getMethod("createFont", new Class[] { Integer.TYPE, File.class });
      runningUnderJava6 = true;
    } catch (SecurityException e) {
      runningUnderJava6 = false;
    } catch (NoSuchMethodException e) {
      runningUnderJava6 = false;
    }
    final File[] files = dir.listFiles();
    for (File f : files) {
      if (f.isDirectory()) {
        registerFonts(f);
        continue;
      }
      if (!f.getName().toLowerCase().trim().endsWith(".ttf")) {
        continue;
      }
      try {
        registerFontFile(f, runningUnderJava6);
      } catch (Throwable e) {
        GPLogger.getLogger(ITextEngine.class).log(Level.FINE, "Failed to register font from " + f.getAbsolutePath(), e);
      }
    }
  }

  private static Font createAwtFont(File fontFile, boolean runningUnderJava6) throws IOException, FontFormatException {
    return runningUnderJava6 ? Font.createFont(Font.TRUETYPE_FONT, fontFile) : Font.createFont(Font.TRUETYPE_FONT,
        new FileInputStream(fontFile));
  }

  private void registerFontFile(final File fontFile, final boolean runningUnderJava6) throws FontFormatException,
      IOException {
    // FontFactory.register(fontFile.getAbsolutePath());
    Font awtFont = createAwtFont(fontFile, runningUnderJava6);

    final String family = awtFont.getFamily().toLowerCase();
    if (myMap_Family_RegularFont.containsKey(family)) {
      return;
    }

    try {
      myMap_Family_ItextFont.put(family, createFontSupplier(fontFile, BaseFont.EMBEDDED));
    } catch (DocumentException e) {
      if (e.getMessage().indexOf("cannot be embedded") < 0) {
        GPLogger.logToLogger(e);
        return;
      }
    }
    try {
      myMap_Family_ItextFont.put(family, createFontSupplier(fontFile, BaseFont.NOT_EMBEDDED));
    } catch (DocumentException e) {
      GPLogger.logToLogger(e);
      return;
    }
    GPLogger.getLogger(getClass()).fine("registering font: " + family);
    myMap_Family_RegularFont.put(family, Suppliers.<Font> memoize(new Supplier<Font>() {
      @Override
      public Font get() {
        try {
          return createAwtFont(fontFile, runningUnderJava6);
        } catch (IOException e) {
          GPLogger.log(e);
        } catch (FontFormatException e) {
          GPLogger.log(e);
        }
        return null;
      }
    }));
  }

  private Function<String, BaseFont> createFontSupplier(final File fontFile, final boolean isEmbedded)
      throws DocumentException, IOException {
    BaseFont.createFont(fontFile.getAbsolutePath(), GanttLanguage.getInstance().getCharSet(), isEmbedded);
    return new Function<String, BaseFont>() {
      @Override
      public BaseFont apply(String charset) {
        try {
          return BaseFont.createFont(fontFile.getAbsolutePath(), charset, isEmbedded);
        } catch (DocumentException e) {
          GPLogger.log(e);
        } catch (IOException e) {
          GPLogger.log(e);
        }
        return null;
      }
    };
  }

  private static class FontKey {
    private String family;
    private int style;
    private float size;
    private String charset;

    FontKey(String family, String charset, int style, float size) {
      this.family = family;
      this.charset = charset;
      this.style = style;
      this.size = size;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((charset == null) ? 0 : charset.hashCode());
      result = prime * result + ((family == null) ? 0 : family.hashCode());
      result = prime * result + Float.floatToIntBits(size);
      result = prime * result + style;
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (getClass() != obj.getClass())
        return false;
      FontKey other = (FontKey) obj;
      if (charset == null) {
        if (other.charset != null)
          return false;
      } else if (!charset.equals(other.charset))
        return false;
      if (family == null) {
        if (other.family != null)
          return false;
      } else if (!family.equals(other.family))
        return false;
      if (Float.floatToIntBits(size) != Float.floatToIntBits(other.size))
        return false;
      if (style != other.style)
        return false;
      return true;
    }
  }

  public com.itextpdf.text.Font getFont(String family, String charset, int style, float size) {
    FontKey key = new FontKey(family, charset, style, size);
    com.itextpdf.text.Font result = myFontCache.get(key);
    if (result == null) {
      Function<String, BaseFont> f = myMap_Family_ItextFont.get(family);
      BaseFont bf = f == null ? getFallbackFont(charset) : f.apply(charset);
      if (bf != null) {
        result = new com.itextpdf.text.Font(bf, size);
        myFontCache.put(key, result);
      } else {
        GPLogger.log(new RuntimeException("Font with family=" + family + " not found. Also tried fallback font"));
      }
    }
    return result;

  }

  public FontMapper getFontMapper(final FontSubstitutionModel substitutions, final String charset) {
    return new FontMapper() {
      private Map<Font, BaseFont> myFontCache = new HashMap<Font, BaseFont>();

      @Override
      public BaseFont awtToPdf(Font awtFont) {
        if (myFontCache.containsKey(awtFont)) {
          return myFontCache.get(awtFont);
        }
        String family = awtFont.getFamily().toLowerCase().replace(' ', '_');
        if (myProperties.containsKey("font." + family)) {
          family = String.valueOf(myProperties.get("font." + family));
        }
        FontSubstitution substitution = substitutions.getSubstitution(family);
        if (substitution != null) {
          family = substitution.getSubstitutionFamily();
        }
        Function<String, BaseFont> f = myMap_Family_ItextFont.get(family);
        if (f != null) {
          BaseFont result = f.apply(charset);
          myFontCache.put(awtFont, result);
          return result;
        }
        BaseFont result = getFallbackFont(charset);
        if (result == null) {
          GPLogger.log(new RuntimeException("Font with family=" + awtFont.getFamily()
              + " not found. Also tried family=" + family + " and fallback font"));
        }
        return result;
      }

      @Override
      public Font pdfToAwt(BaseFont itextFont, int size) {
        return null;
      }

    };
  }

  protected BaseFont getFallbackFont(String charset) {
    if (myFallbackFont == null) {
      try {
        myFallbackFont = BaseFont.createFont(Platform.resolve(getClass().getResource(FALLBACK_FONT_PATH)).getPath(),
            charset, BaseFont.EMBEDDED);
      } catch (DocumentException e) {
        GPLogger.logToLogger(e);
      } catch (IOException e) {
        GPLogger.logToLogger(e);
      }
    }
    return myFallbackFont;
  }

  public void setProperties(Properties properties) {
    myProperties = properties;
  }
}
