package fi.vm.sade.ajastuspalvelu.service.converter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import fi.vm.sade.ajastuspalvelu.dao.ScheduledTaskDao;
import fi.vm.sade.ajastuspalvelu.model.ScheduledTask;
import fi.vm.sade.ajastuspalvelu.service.dto.ScheduledTaskDto;

@Component("scheduledTaskConverter")
public class ScheduledTaskConverter implements Converter<ScheduledTask, ScheduledTaskDto> {

    private final ScheduledTaskDao dao;
    
    @Autowired
    public ScheduledTaskConverter(ScheduledTaskDao dao) {
        this.dao = dao;
    }
    
    @Override
    public ScheduledTask convertToModel(ScheduledTaskDto dto) {
        ScheduledTask task = getScheduledTask(dto);
        //TODO task.setTask(task);
        task.setHakuOid(dto.hakuOid);
        task.setRuntimeForSingle(dto.runtimeForSingle);
        return task;
    }

    @Override
    public ScheduledTaskDto convertToDto(ScheduledTask model) {
        return new ScheduledTaskDto(model.getId(), model.getTask().getName(), model.getHakuOid(), model.getRuntimeForSingle());
    }

    private ScheduledTask getScheduledTask(ScheduledTaskDto dto) {
        if (dto.id != null) {
            return dao.read(dto.id);
        }
        return new ScheduledTask();
    }
}
