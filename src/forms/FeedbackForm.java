package forms;

import dao.FeedbackDAO;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.sql.SQLException;
import javax.swing.JPanel;
import models.Feedback;
import utils.StarRating;
import utils.Ui;

// rate a completed session (new or edit)
public final class FeedbackForm {

    private FeedbackForm() { }

    // returns true when the feedback was saved
    public static boolean show(Component parent, Feedback f, boolean isNew) {
        StarRating stars = new StarRating(f.getRating() == 0 ? 5 : f.getRating());
        javax.swing.JTextArea comments = Ui.area(4, 30);
        comments.setText(f.getComments() == null ? "" : f.getComments());

        JPanel body = new JPanel(new BorderLayout(0, 10));
        body.add(Ui.label("How was your session?", java.awt.Font.BOLD, 15f, utils.Theme.NAVY), BorderLayout.NORTH);
        JPanel center = new JPanel(new BorderLayout(0, 8));
        center.add(stars, BorderLayout.NORTH);
        center.add(Ui.scrolled(comments), BorderLayout.CENTER);
        body.add(center, BorderLayout.CENTER);
        body.setPreferredSize(new Dimension(380, 220));

        return Ui.ask(parent, isNew ? "Give feedback" : "Edit feedback", body, () -> {
            f.setRating(stars.getRating());
            f.setComments(comments.getText().trim());
            try {
                if (isNew) { new FeedbackDAO().create(f); } else { new FeedbackDAO().update(f); }
                return null;
            } catch (SQLException ex) {
                return Ui.friendly(ex);
            } catch (IllegalArgumentException ex) {
                return ex.getMessage();
            }
        });
    }
}
