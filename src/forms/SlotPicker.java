package forms;

import dao.AppointmentDAO;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridLayout;
import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.DefaultListCellRenderer;
import utils.Theme;
import utils.Ui;

// pick a day and one of the free hours of a counsellor (this is the "check availability" step)
public class SlotPicker extends JPanel {

    private final AppointmentDAO dao = new AppointmentDAO();
    private final JComboBox<LocalDate> dateBox = new JComboBox<>();
    private final JComboBox<LocalTime> timeBox = new JComboBox<>();
    private final JLabel hint = Ui.muted(" ");
    private int counsellorId;

    public SlotPicker() {
        super(new GridLayout(0, 1, 0, 6));
        setOpaque(false);

        // the next four weeks, weekdays only
        LocalDate d = LocalDate.now();
        for (int i = 0; i < 28; i++, d = d.plusDays(1)) {
            if (d.getDayOfWeek() != DayOfWeek.SATURDAY && d.getDayOfWeek() != DayOfWeek.SUNDAY) { dateBox.addItem(d); }
        }
        dateBox.setFont(Theme.font(Font.PLAIN, 14f));
        timeBox.setFont(Theme.font(Font.PLAIN, 14f));
        dateBox.setRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> l, Object v, int i, boolean s, boolean f) {
                return super.getListCellRendererComponent(l, v == null ? "" : ((LocalDate) v).format(Ui.DATE), i, s, f);
            }
        });
        timeBox.setRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> l, Object v, int i, boolean s, boolean f) {
                return super.getListCellRendererComponent(l, v == null ? "" : ((LocalTime) v).format(Ui.TIME), i, s, f);
            }
        });
        dateBox.addActionListener(e -> reload());
        add(dateBox);
        add(timeBox);
        add(hint);
    }

    // choose a counsellor and jump to the first day that has a free hour
    public void setCounsellor(int counsellorId) {
        this.counsellorId = counsellorId;
        try {
            for (int i = 0; i < dateBox.getItemCount(); i++) {
                if (!dao.freeSlots(counsellorId, dateBox.getItemAt(i)).isEmpty()) {
                    if (dateBox.getSelectedIndex() != i) { dateBox.setSelectedIndex(i); return; }   // the selection change reloads the hours
                    break;
                }
            }
        } catch (SQLException ex) {
            hint.setText(Ui.friendly(ex));
        }
        reload();
    }

    // read the free hours for the chosen day
    public void reload() {
        LocalDate date = getDate();
        if (date == null || counsellorId == 0) { return; }
        try {
            List<LocalTime> free = dao.freeSlots(counsellorId, date);
            timeBox.setModel(new DefaultComboBoxModel<>(free.toArray(new LocalTime[0])));
            hint.setText(free.isEmpty() ? "No free hours on this day." : free.size() + " free hour(s) on this day");
        } catch (SQLException ex) {
            hint.setText(Ui.friendly(ex));
        }
    }

    public LocalDate getDate() { return (LocalDate) dateBox.getSelectedItem(); }
    public LocalTime getTime() { return (LocalTime) timeBox.getSelectedItem(); }
}
