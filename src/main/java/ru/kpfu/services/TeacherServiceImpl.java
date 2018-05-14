package ru.kpfu.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import ru.kpfu.dtos.LessonDto;
import ru.kpfu.dtos.MarkDto;
import ru.kpfu.dtos.UserDto;
import ru.kpfu.forms.AddHomeworkForm;
import ru.kpfu.forms.AddMarkForm;
import ru.kpfu.models.Homework;
import ru.kpfu.models.Lesson;
import ru.kpfu.models.Mark;
import ru.kpfu.models.User;
import ru.kpfu.repositories.HomeworkRepository;
import ru.kpfu.repositories.LessonRepository;
import ru.kpfu.repositories.MarkRepository;
import ru.kpfu.repositories.UserRepository;

import java.util.*;

@Service
public class TeacherServiceImpl implements TeacherService{
    private final DateUtil dateUtil;
    private final UserRepository userRepository;
    private final LessonRepository lessonRepository;
    private final HomeworkRepository homeworkRepository;
    private final MarkRepository markRepository;

    @Autowired
    public TeacherServiceImpl(DateUtil dateUtil, UserRepository userRepository, LessonRepository lessonRepository, HomeworkRepository homeworkRepository, MarkRepository markRepository) {
        this.dateUtil = dateUtil;
        this.userRepository = userRepository;
        this.lessonRepository = lessonRepository;
        this.homeworkRepository = homeworkRepository;
        this.markRepository = markRepository;
    }

    @Override
    public List<LessonDto> getTimetable(String login) {
        User teacher = userRepository.findByLogin(login).orElseThrow(() -> new DataAccessException("Ошибка в БД") {
        });
        List<LessonDto> list = new ArrayList<>();
        List<Lesson> lessons = lessonRepository.getByTeacher(teacher);
        Collections.sort(lessons);
        lessons.forEach(l -> list.add(LessonDto.buildFrom(l)));
        return list;
    }

    @Override
    public List<MarkDto> getMarksByLessonAndDate(String lessonId, Date date) {
        List<MarkDto> markDtos = new ArrayList<>();
        Lesson lesson = lessonRepository.findOne(Long.parseLong(lessonId));
        markRepository.findByLessonAndDate(lesson, date).forEach(m -> markDtos.add(MarkDto.buildFrom(m)));
        return markDtos;
    }

    @Override
    public Homework getHomeworkByLessonIdAndDate(String lessonId, Date date) {
        Lesson lesson = lessonRepository.findOne(Long.parseLong(lessonId));
        return homeworkRepository.findByLessonAndDate(lesson, date).orElse(null);
    }

    @Override
    public List<UserDto> getStudentsByLesson(String lessonId) {
        List<UserDto> userDtos = new ArrayList<>();
        Lesson lesson = lessonRepository.findOne(Long.parseLong(lessonId));
        userRepository.findByStudentClass(lesson.getStudentClass()).forEach(u -> userDtos.add(UserDto.buildFrom(u)));
        return userDtos;
    }

    @Override
    public List<LessonDto> getLessonsByDate(String login, Date date) {
        User teacher = userRepository.findByLogin(login).orElseThrow(() -> new DataAccessException("Ошибка в БД") {});
        List<LessonDto> list = new ArrayList<>();
        List<Lesson> lessons = lessonRepository.getByWeekdayAndTeacher(dateUtil.getDayOfWeek(date), teacher);
        Collections.sort(lessons);
        lessons.forEach(l -> list.add(LessonDto.buildFrom(l)));
        return list;
    }

    @Override
    public void saveMark(AddMarkForm form) {
        Lesson lesson = lessonRepository.findOne(Long.parseLong(form.getLessonId()));
        Date date = dateUtil.convertFromString(DateUtil.STRING_DATE_TYPE_ISO, form.getDate());
        User student = userRepository.findOne(Long.parseLong(form.getStudentId()));
        Optional<Mark> optionalMark = markRepository.findByLessonAndDateAndStudent(lesson, date, student);
        Mark mark;
        if(optionalMark.isPresent()) {
            mark = optionalMark.get();
            mark.setValue(form.getValue());
        } else {
            mark = Mark.builder()
                    .lesson(lesson)
                    .date(date)
                    .student(student)
                    .value(form.getValue())
                    .build();
        }
        markRepository.save(mark);
    }

    @Override
    public void saveHomework(AddHomeworkForm form) {
        Lesson lesson = lessonRepository.findOne(Long.parseLong(form.getLessonId()));
        Date date = dateUtil.convertFromString(DateUtil.STRING_DATE_TYPE_ISO, form.getDate());
        Optional<Homework> optionalHomework = homeworkRepository.findByLessonAndDate(lesson, date);
        Homework homework;
        if(optionalHomework.isPresent()){
            homework = optionalHomework.get();
            homework.setText(form.getHomeworkText());
        } else {
            homework = optionalHomework.orElse(Homework.builder()
                    .date(date)
                    .lesson(lesson)
                    .text(form.getHomeworkText())
                    .build());
        }
        homeworkRepository.save(homework);
    }
}
