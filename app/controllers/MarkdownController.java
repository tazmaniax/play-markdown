package controllers;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.HashSet;
import java.util.Set;

import markdown.Markdown;
import play.Play;
import play.i18n.Lang;
import play.jobs.Job;
import play.jobs.OnApplicationStart;
import play.mvc.Controller;

public class MarkdownController extends Controller {

	private static String DEF_ROOT = "public/mddocs";
	private static String DEF_DEF_LANG = "en";
	private static String root = null;
	private static String root() {
	    if (null == root) {
            root = Play.configuration.getProperty("markdown.root", DEF_ROOT);
            if (!root.endsWith("/") && !root.endsWith("\\")) {
                root = root + "/";
            }
	    }
	    return root;
	}
    private static String defLang = null;
	private static String defLang() {
	    if (null == defLang) {
	        defLang = Play.configuration.getProperty("markdown.defLang", DEF_DEF_LANG);
	    }
	    return defLang;
	}
	private static String img = null;
	private static String img() {
	    if (null == img) {
            img = Play.configuration.getProperty("markdown.imgDir", "images");
	    }
	    return img;
	}

    private static Set<String> invalidLangs = new HashSet<String>();

	private static String docDir() {
        String lang = Lang.get();
        if (null == lang) return root();
        else if (invalidLangs.contains(lang)) return docDirWithDefLang();
        else return root() + lang + "/";
    }

    private static String docDirWithDefLang() {
        String s = defLang();
        if (invalidLangs.contains(s)) return docDirWithoutLang();
        return root() + s + "/";
    }

    private static String docDirWithoutLang() {
        return root + "/";
    }

    private static String imgDir() {
        return docDir() + img() + "/";
    }

    private static String imgDirWithDefLang() {
        return docDirWithDefLang() + img() + "/";
    }

    private static String imgDirWithoutLang() {
        return docDirWithoutLang() + img() + "/";
    }

	public static void transform(String page) throws Exception {
	    if (page.endsWith(".md")) page = page.substring(0, page.length() - 3);

		// Just a little validation to make sure the path is not forged
		if (page == null || page.indexOf('/') > 0 || page.indexOf('\\') > 0
				|| page.indexOf('.') > 0)
			throw new IOException("Invalid path:"+page);

		File f = new File(Play.applicationPath, docDir() + page
				+ ".md");
		if (!f.exists()) {
		    // try defLang
		    invalidLangs.add(Lang.get());
		    f = new File(Play.applicationPath, docDirWithDefLang() + page + ".md");
		    if (!f.exists()) {
                invalidLangs.add(defLang());
                f = new File(Play.applicationPath, docDirWithoutLang() + page + ".md");
                if (!f.exists()) {
                    notFound("Markdown page for " + page + " not found");
                }
		    }
		}

		Reader pageReader = new FileReader(f);
		String html = Markdown.transformMarkdown(pageReader);
		render(html);
	}

	public static void image(String imageName, String ext) throws Exception {
		// Just a little validation to make sure the path is not forged
		if (imageName == null || imageName.indexOf('/') > 0
				|| imageName.indexOf('\\') > 0 || imageName.indexOf('.') > 0)
			throw new IOException("Invalid path:"+imageName);
		if (ext == null || ext.indexOf('/') > 0 || ext.indexOf('\\') > 0
				|| ext.indexOf('.') > 0)
			throw new IOException("Invalid path:"+ext);

		File image = new File(Play.applicationPath, imgDir() + imageName
				+ '.' + ext);

		if (!image.exists()) {
            // try defLang
            invalidLangs.add(Lang.get());
            image = new File(Play.applicationPath, imgDirWithDefLang() + imageName + "." + ext);
            if (!image.exists()) {
                invalidLangs.add(defLang());
                image = new File(Play.applicationPath, imgDirWithoutLang() + imageName + "." + ext);
                if (!image.exists()) {
                    notFound();
                }
            }
		}
		renderBinary(image);
	}

    /**
     * Force to check if language folder version is ready
     */
	public static void refreshLangs() {
	    invalidLangs.clear();;
	}

}
